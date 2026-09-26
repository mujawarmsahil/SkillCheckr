package com.skillcheckr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AuthRepositoryImpl implements AuthRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> login(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setRole(rs.getString("user_role"));
                u.setProfileImage(rs.getString("profile_image"));
                u.setAuthProvider(rs.getString("auth_provider"));
                u.setProviderId(rs.getString("provider_id"));
                return u;
            }
        }, username);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.get(0);
        String storedPassword = user.getPassword();

        // Support BCrypt hashed matching with fallback to plain-text (for legacy/seed accounts)
        boolean matches = false;
        if (storedPassword != null) {
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                    || storedPassword.startsWith("$2y$")) {
                matches = passwordEncoder.matches(password, storedPassword);
            } else {
                matches = storedPassword.equals(password);
                // Automatically upgrade plain text password to BCrypt hash in database
                if (matches) {
                    try {
                        String newHash = passwordEncoder.encode(password);
                        jdbcTemplate.update("UPDATE user SET password = ? WHERE user_id = ?",
                                newHash, user.getUserId());
                        user.setPassword(newHash);
                    } catch (Exception ignored) {
                        // Non-critical auto-upgrade failure
                    }
                }
            }
        }

        return matches ? Optional.of(user) : Optional.empty();
    }

    @Override
    public int getStudentIdByUserId(int userId) {
        String sql = "SELECT student_id FROM student WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Override
    public int getTeacherIdByUserId(int userId) {
        String sql = "SELECT teacher_id FROM teacher WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Override
    public int getAdminIdByUserId(int userId) {
        String sql = "SELECT admin_id FROM admin WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Override
    public Optional<UserProfileDTO> getUserProfile(int userId) {
        String userSql = "SELECT * FROM user WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(userSql, (rs, rowNum) -> {
            User u = new User();
            u.setUserId(rs.getInt("user_id"));
            u.setUsername(rs.getString("username"));
            u.setRole(rs.getString("user_role"));
            u.setProfileImage(rs.getString("profile_image"));
            u.setAuthProvider(rs.getString("auth_provider"));
            u.setProviderId(rs.getString("provider_id"));
            return u;
        }, userId);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.get(0);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserId(user.getUserId());
        profile.setUsername(user.getUsername());
        profile.setRole(user.getRole());
        profile.setProfileImage(user.getProfileImage());

        String role = user.getRole() != null ? user.getRole().trim().toLowerCase() : "";

        if ("student".equals(role)) {
            jdbcTemplate.query("SELECT * FROM student WHERE user_id = ?", rs -> {
                profile.setRoleId(rs.getInt("student_id"));
                profile.setName(rs.getString("name"));
                profile.setEmail(rs.getString("email"));
                profile.setContact(rs.getString("contact"));
                String img = rs.getString("profile_image");
                if (img != null && !img.trim().isEmpty()) {
                    profile.setProfileImage(img);
                }
            }, userId);
        } else if ("teacher".equals(role)) {
            jdbcTemplate.query("SELECT * FROM teacher WHERE user_id = ?", rs -> {
                profile.setRoleId(rs.getInt("teacher_id"));
                profile.setName(rs.getString("name"));
                profile.setEmail(rs.getString("email"));
                profile.setContact(rs.getString("contact"));
                String img = rs.getString("profile_image");
                if (img != null && !img.trim().isEmpty()) {
                    profile.setProfileImage(img);
                }
            }, userId);
        } else if ("admin".equals(role)) {
            jdbcTemplate.query("SELECT * FROM admin WHERE user_id = ?", rs -> {
                profile.setRoleId(rs.getInt("admin_id"));
                profile.setName(rs.getString("name"));
                profile.setEmail(rs.getString("email"));
                profile.setContact(rs.getString("contact"));
                String img = rs.getString("profile_image");
                if (img != null && !img.trim().isEmpty()) {
                    profile.setProfileImage(img);
                }
            }, userId);
        }

        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            profile.setName(user.getUsername());
        }
        if (profile.getEmail() == null) {
            profile.setEmail("");
        }
        if (profile.getContact() == null) {
            profile.setContact("");
        }

        return Optional.of(profile);
    }

    @Override
    public Optional<UserProfileDTO> updateUserProfile(UserProfileDTO profile) {
        if (profile == null || profile.getUserId() <= 0) {
            return Optional.empty();
        }

        try {
            if (profile.getPassword() != null && !profile.getPassword().trim().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(profile.getPassword().trim());
                jdbcTemplate.update(
                        "UPDATE user SET username = ?, password = ?, profile_image = ? WHERE user_id = ?",
                        profile.getUsername(), encodedPassword, profile.getProfileImage(), profile.getUserId());
            } else {
                jdbcTemplate.update(
                        "UPDATE user SET username = ?, profile_image = ? WHERE user_id = ?",
                        profile.getUsername(), profile.getProfileImage(), profile.getUserId());
            }

            String roleSql = "SELECT user_role FROM user WHERE user_id = ?";
            String role = jdbcTemplate.queryForObject(roleSql, String.class, profile.getUserId());
            String roleStr = role != null ? role.trim().toLowerCase() : "";

            if ("student".equals(roleStr)) {
                updateOrInsertRole(profile, "student");
            } else if ("teacher".equals(roleStr)) {
                updateOrInsertRole(profile, "teacher");
            } else if ("admin".equals(roleStr)) {
                updateOrInsertRole(profile, "admin");
            }

            // Sync updated profile to request table as well
            try {
                jdbcTemplate.update(
                        "UPDATE request SET name = ?, email = ?, contact = ? WHERE username = ?",
                        profile.getName(), profile.getEmail(), profile.getContact(), profile.getUsername());
            } catch (Exception ignored) {}

            return getUserProfile(profile.getUserId());
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            return Optional.empty();
        }
    }

    private void updateOrInsertRole(UserProfileDTO profile, String role) {
        int updated = jdbcTemplate.update(
                "UPDATE " + role + " SET name = ?, email = ?, contact = ?, profile_image = ? WHERE user_id = ?",
                profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO " + role + " (user_id, name, contact, email, profile_image) VALUES (?, ?, ?, ?, ?)",
                    profile.getUserId(), profile.getName(), profile.getContact(), profile.getEmail(), profile.getProfileImage());
        }
    }

    @Override
    public boolean isUsernameInUse(String username, int excludeUserId) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ? AND user_id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, excludeUserId);
        return count != null && count > 0;
    }

    @Override
    public boolean isEmailInUse(String email, int excludeUserId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        for (String table : new String[]{"student", "teacher", "admin"}) {
            String sql = "SELECT COUNT(*) FROM " + table + " WHERE email = ? AND (user_id != ? OR user_id IS NULL)";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, trimmed, excludeUserId);
            if (count != null && count > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean verifyCurrentPassword(int userId, String oldPassword) {
        if (oldPassword == null || oldPassword.isEmpty() || userId <= 0) {
            return false;
        }
        String sql = "SELECT password FROM user WHERE user_id = ?";
        try {
            String storedPassword = jdbcTemplate.queryForObject(sql, String.class, userId);
            if (storedPassword == null) {
                return false;
            }
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                    || storedPassword.startsWith("$2y$")) {
                return passwordEncoder.matches(oldPassword, storedPassword);
            } else {
                return storedPassword.equals(oldPassword);
            }
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}