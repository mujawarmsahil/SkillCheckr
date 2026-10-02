package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AuthRepositoryImpl implements AuthRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM user WHERE LOWER(username) = LOWER(?)";
        List<User> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
            User u = new User();
            u.setUserId(rs.getInt("user_id"));
            u.setUsername(rs.getString("username"));
            u.setPassword(rs.getString("password"));
            u.setRole(rs.getString("user_role"));
            u.setProfileImage(rs.getString("profile_image"));
            u.setAuthProvider(rs.getString("auth_provider"));
            u.setProviderId(rs.getString("provider_id"));
            u.setStatus(rs.getString("status"));
            return u;
        }, username.trim());

        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public boolean updatePassword(int userId, String passwordHash) {
        int updated = jdbcTemplate.update("UPDATE user SET password = ? WHERE user_id = ?",
                passwordHash, userId);
        return updated > 0;
    }

    @Override
    public Optional<String> findPasswordByUserId(int userId) {
        if (userId <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT password FROM user WHERE user_id = ?";
        try {
            String storedPassword = jdbcTemplate.queryForObject(sql, String.class, userId);
            return Optional.ofNullable(storedPassword);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
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

        if (profile.getPassword() != null && !profile.getPassword().trim().isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE user SET username = ?, password = ?, profile_image = ? WHERE user_id = ?",
                    profile.getUsername(), profile.getPassword().trim(), profile.getProfileImage(), profile.getUserId());
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
        jdbcTemplate.update(
                "UPDATE request SET name = ?, email = ?, contact = ? WHERE username = ?",
                profile.getName(), profile.getEmail(), profile.getContact(), profile.getUsername());

        return getUserProfile(profile.getUserId());
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
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?) AND user_id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, excludeUserId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
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
    public boolean isUserActive(int userId) {
        if (userId <= 0) {
            return false;
        }
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM user WHERE user_id = ?",
                (rs, rowNum) -> rs.getString("status"),
                userId);
        if (statuses.isEmpty()) {
            return false;
        }
        String status = statuses.get(0);
        return status == null || status.trim().isEmpty() || !"Inactive".equalsIgnoreCase(status.trim());
    }
}