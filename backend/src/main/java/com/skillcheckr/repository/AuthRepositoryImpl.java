package com.skillcheckr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

@Repository
public class AuthRepositoryImpl implements AuthRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try {
            List<User> users = jdbcTemplate.query(sql, new RowMapper<User>() {
                @Override
                public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setUsername(rs.getString("username"));
                    u.setPassword(rs.getString("password"));
                    u.setRole(rs.getString("user_role"));
                    try {
                        u.setProfileImage(rs.getString("profile_image"));
                    } catch (Exception ignored) {
                    }
                    try {
                        u.setAuthProvider(rs.getString("auth_provider"));
                    } catch (Exception ignored) {
                    }
                    try {
                        u.setProviderId(rs.getString("provider_id"));
                    } catch (Exception ignored) {
                    }
                    return u;
                }
            }, username);

            if (users.isEmpty()) {
                return null;
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

            return matches ? user : null;
        } catch (EmptyResultDataAccessException e) {
            return null;
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

    private String getFirstAvailableColumn(ResultSet rs, String... candidateColumns) {
        for (String col : candidateColumns) {
            try {
                String val = rs.getString(col);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public UserProfileDTO getUserProfile(int userId) {
        String userSql = "SELECT * FROM user WHERE user_id = ?";
        try {
            List<User> users = jdbcTemplate.query(userSql, (rs, rowNum) -> {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setRole(rs.getString("user_role"));
                try {
                    u.setProfileImage(rs.getString("profile_image"));
                } catch (Exception ignored) {
                }
                try {
                    u.setAuthProvider(rs.getString("auth_provider"));
                } catch (Exception ignored) {
                }
                try {
                    u.setProviderId(rs.getString("provider_id"));
                } catch (Exception ignored) {
                }
                return u;
            }, userId);

            if (users.isEmpty()) {
                return null;
            }

            User user = users.get(0);
            UserProfileDTO profile = new UserProfileDTO();
            profile.setUserId(user.getUserId());
            profile.setUsername(user.getUsername());
            profile.setRole(user.getRole());
            profile.setProfileImage(user.getProfileImage());

            String role = user.getRole() != null ? user.getRole().trim().toLowerCase() : "";

            if ("student".equals(role)) {
                try {
                    jdbcTemplate.query("SELECT * FROM student WHERE user_id = ?", rs -> {
                        profile.setRoleId(rs.getInt("student_id"));
                        String n = getFirstAvailableColumn(rs, "name", "student_name");
                        if (n != null) profile.setName(n);
                        String em = getFirstAvailableColumn(rs, "email", "student_email");
                        if (em != null) profile.setEmail(em);
                        String c = getFirstAvailableColumn(rs, "contact", "student_contact");
                        if (c != null) profile.setContact(c);
                        try {
                            String img = rs.getString("profile_image");
                            if (img != null && !img.trim().isEmpty()) {
                                profile.setProfileImage(img);
                            }
                        } catch (Exception ignored) {
                        }
                    }, userId);
                } catch (Exception ignored) {
                }
            } else if ("teacher".equals(role)) {
                try {
                    jdbcTemplate.query("SELECT * FROM teacher WHERE user_id = ?", rs -> {
                        profile.setRoleId(rs.getInt("teacher_id"));
                        String n = getFirstAvailableColumn(rs, "name", "teacher_name");
                        if (n != null) profile.setName(n);
                        String em = getFirstAvailableColumn(rs, "email", "teacher_email");
                        if (em != null) profile.setEmail(em);
                        String c = getFirstAvailableColumn(rs, "contact", "teacher_contact");
                        if (c != null) profile.setContact(c);
                        try {
                            String img = rs.getString("profile_image");
                            if (img != null && !img.trim().isEmpty()) {
                                profile.setProfileImage(img);
                            }
                        } catch (Exception ignored) {
                        }
                    }, userId);
                } catch (Exception ignored) {
                }
            } else if ("admin".equals(role)) {
                try {
                    jdbcTemplate.query("SELECT * FROM admin WHERE user_id = ?", rs -> {
                        profile.setRoleId(rs.getInt("admin_id"));
                        String n = getFirstAvailableColumn(rs, "name", "admin_name");
                        if (n != null) profile.setName(n);
                        String em = getFirstAvailableColumn(rs, "email", "admin_email");
                        if (em != null) profile.setEmail(em);
                        String c = getFirstAvailableColumn(rs, "contact", "admin_contact");
                        if (c != null) profile.setContact(c);
                        try {
                            String img = rs.getString("profile_image");
                            if (img != null && !img.trim().isEmpty()) {
                                profile.setProfileImage(img);
                            }
                        } catch (Exception ignored) {
                        }
                    }, userId);
                } catch (Exception ignored) {
                }
            }

            // Fallback 1: If email is still missing, check all three role tables by user_id
            if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                try {
                    jdbcTemplate.query("SELECT * FROM student WHERE user_id = ?", rs -> {
                        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                            profile.setEmail(getFirstAvailableColumn(rs, "email", "student_email"));
                        }
                        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                            profile.setName(getFirstAvailableColumn(rs, "name", "student_name"));
                        }
                        if (profile.getContact() == null || profile.getContact().trim().isEmpty()) {
                            profile.setContact(getFirstAvailableColumn(rs, "contact", "student_contact"));
                        }
                    }, userId);
                } catch (Exception ignored) {}
            }
            if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                try {
                    jdbcTemplate.query("SELECT * FROM teacher WHERE user_id = ?", rs -> {
                        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                            profile.setEmail(getFirstAvailableColumn(rs, "email", "teacher_email"));
                        }
                        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                            profile.setName(getFirstAvailableColumn(rs, "name", "teacher_name"));
                        }
                        if (profile.getContact() == null || profile.getContact().trim().isEmpty()) {
                            profile.setContact(getFirstAvailableColumn(rs, "contact", "teacher_contact"));
                        }
                    }, userId);
                } catch (Exception ignored) {}
            }
            if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                try {
                    jdbcTemplate.query("SELECT * FROM admin WHERE user_id = ?", rs -> {
                        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                            profile.setEmail(getFirstAvailableColumn(rs, "email", "admin_email"));
                        }
                        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                            profile.setName(getFirstAvailableColumn(rs, "name", "admin_name"));
                        }
                        if (profile.getContact() == null || profile.getContact().trim().isEmpty()) {
                            profile.setContact(getFirstAvailableColumn(rs, "contact", "admin_contact"));
                        }
                    }, userId);
                } catch (Exception ignored) {}
            }

            // Fallback 2: Check the registration requests table by username
            if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                try {
                    jdbcTemplate.query("SELECT * FROM request WHERE username = ? ORDER BY request_id DESC LIMIT 1", rs -> {
                        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
                            profile.setEmail(getFirstAvailableColumn(rs, "email"));
                        }
                        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
                            profile.setName(getFirstAvailableColumn(rs, "name"));
                        }
                        if (profile.getContact() == null || profile.getContact().trim().isEmpty()) {
                            profile.setContact(getFirstAvailableColumn(rs, "contact"));
                        }
                    }, user.getUsername());
                } catch (Exception ignored) {}
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

            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    private volatile boolean profileImageColumnsChecked = false;

    private synchronized void ensureProfileImageColumnsExist() {
        if (profileImageColumnsChecked) {
            return;
        }
        for (String table : new String[]{"user", "student", "teacher", "admin"}) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN profile_image VARCHAR(1000) NULL");
            } catch (Exception ignored) {
            }
        }
        profileImageColumnsChecked = true;
    }

    @Override
    public UserProfileDTO updateUserProfile(UserProfileDTO profile) {
        if (profile == null || profile.getUserId() <= 0) {
            return null;
        }

        try {
            ensureProfileImageColumnsExist();

            // Update user table
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

            // Determine role
            String roleSql = "SELECT user_role FROM user WHERE user_id = ?";
            String role = jdbcTemplate.queryForObject(roleSql, String.class, profile.getUserId());
            String roleStr = role != null ? role.trim().toLowerCase() : "";

            if ("student".equals(roleStr)) {
                int updated = 0;
                try {
                    updated = jdbcTemplate.update(
                            "UPDATE student SET name = ?, email = ?, contact = ?, profile_image = ? WHERE user_id = ?",
                            profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                } catch (Exception e) {
                    try {
                        updated = jdbcTemplate.update(
                                "UPDATE student SET student_name = ?, student_email = ?, student_contact = ?, profile_image = ? WHERE user_id = ?",
                                profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                    } catch (Exception ignored) {}
                }
                if (updated == 0) {
                    try {
                        jdbcTemplate.update(
                                "INSERT INTO student (user_id, name, contact, email, profile_image) VALUES (?, ?, ?, ?, ?)",
                                profile.getUserId(), profile.getName(), profile.getContact(), profile.getEmail(), profile.getProfileImage());
                    } catch (Exception ignored) {}
                }
            } else if ("teacher".equals(roleStr)) {
                int updated = 0;
                try {
                    updated = jdbcTemplate.update(
                            "UPDATE teacher SET name = ?, email = ?, contact = ?, profile_image = ? WHERE user_id = ?",
                            profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                } catch (Exception e) {
                    try {
                        updated = jdbcTemplate.update(
                                "UPDATE teacher SET teacher_name = ?, teacher_email = ?, teacher_contact = ?, profile_image = ? WHERE user_id = ?",
                                profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                    } catch (Exception ignored) {}
                }
                if (updated == 0) {
                    try {
                        jdbcTemplate.update(
                                "INSERT INTO teacher (user_id, name, contact, email, profile_image) VALUES (?, ?, ?, ?, ?)",
                                profile.getUserId(), profile.getName(), profile.getContact(), profile.getEmail(), profile.getProfileImage());
                    } catch (Exception ignored) {}
                }
            } else if ("admin".equals(roleStr)) {
                int updated = 0;
                try {
                    updated = jdbcTemplate.update(
                            "UPDATE admin SET name = ?, email = ?, contact = ?, profile_image = ? WHERE user_id = ?",
                            profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                } catch (Exception e) {
                    try {
                        updated = jdbcTemplate.update(
                                "UPDATE admin SET admin_name = ?, admin_email = ?, admin_contact = ?, profile_image = ? WHERE user_id = ?",
                                profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
                    } catch (Exception ignored) {}
                }
                if (updated == 0) {
                    try {
                        jdbcTemplate.update(
                                "INSERT INTO admin (user_id, name, contact, email, profile_image) VALUES (?, ?, ?, ?, ?)",
                                profile.getUserId(), profile.getName(), profile.getContact(), profile.getEmail(), profile.getProfileImage());
                    } catch (Exception ignored) {}
                }
            }

            // Sync updated profile to request table as well
            try {
                jdbcTemplate.update(
                        "UPDATE request SET name = ?, email = ?, contact = ? WHERE username = ?",
                        profile.getName(), profile.getEmail(), profile.getContact(), profile.getUsername());
            } catch (Exception ignored) {}

            return getUserProfile(profile.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isUsernameInUse(String username, int excludeUserId) {
        try {
            String sql = "SELECT COUNT(*) FROM user WHERE username = ? AND user_id != ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username, excludeUserId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEmailInUse(String email, int excludeUserId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        try {
            int studentCount = countEmailInTable("student", new String[]{"email", "student_email"}, trimmed, excludeUserId);
            if (studentCount > 0) return true;

            int teacherCount = countEmailInTable("teacher", new String[]{"email", "teacher_email"}, trimmed, excludeUserId);
            if (teacherCount > 0) return true;

            int adminCount = countEmailInTable("admin", new String[]{"email", "admin_email"}, trimmed, excludeUserId);
            if (adminCount > 0) return true;

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private int countEmailInTable(String tableName, String[] emailColumns, String email, int excludeUserId) {
        for (String col : emailColumns) {
            try {
                String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + col + " = ? AND (user_id != ? OR user_id IS NULL)";
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, excludeUserId);
                if (count != null && count > 0) {
                    return count;
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
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
        } catch (Exception e) {
            return false;
        }
    }
}
