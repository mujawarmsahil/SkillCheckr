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
                        profile.setName(rs.getString("name"));
                        profile.setEmail(rs.getString("email"));
                        profile.setContact(rs.getString("contact"));
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
                        profile.setName(rs.getString("teacher_name"));
                        profile.setEmail(rs.getString("teacher_email"));
                        profile.setContact(rs.getString("teacher_contact"));
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
                        profile.setName(rs.getString("admin_name"));
                        profile.setEmail(rs.getString("admin_email"));
                        profile.setContact(rs.getString("admin_contact"));
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

            if (profile.getName() == null || profile.getName().isEmpty()) {
                profile.setName(user.getUsername());
            }

            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public UserProfileDTO updateUserProfile(UserProfileDTO profile) {
        if (profile == null || profile.getUserId() <= 0) {
            return null;
        }

        try {
            // Ensure profile_image column exists in user table if not already added
            try {
                jdbcTemplate.execute("ALTER TABLE user ADD COLUMN profile_image VARCHAR(1000) NULL");
            } catch (Exception ignored) {
            }

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
                try {
                    jdbcTemplate.execute("ALTER TABLE student ADD COLUMN profile_image VARCHAR(1000) NULL");
                } catch (Exception ignored) {
                }
                jdbcTemplate.update(
                        "UPDATE student SET name = ?, email = ?, contact = ?, profile_image = ? WHERE user_id = ?",
                        profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
            } else if ("teacher".equals(roleStr)) {
                try {
                    jdbcTemplate.execute("ALTER TABLE teacher ADD COLUMN profile_image VARCHAR(1000) NULL");
                } catch (Exception ignored) {
                }
                jdbcTemplate.update(
                        "UPDATE teacher SET teacher_name = ?, teacher_email = ?, teacher_contact = ?, profile_image = ? WHERE user_id = ?",
                        profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
            } else if ("admin".equals(roleStr)) {
                try {
                    jdbcTemplate.execute("ALTER TABLE admin ADD COLUMN profile_image VARCHAR(1000) NULL");
                } catch (Exception ignored) {
                }
                jdbcTemplate.update(
                        "UPDATE admin SET admin_name = ?, admin_email = ?, admin_contact = ?, profile_image = ? WHERE user_id = ?",
                        profile.getName(), profile.getEmail(), profile.getContact(), profile.getProfileImage(), profile.getUserId());
            }

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
        try {
            Integer studentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM student WHERE email = ? AND user_id != ?",
                    Integer.class, email, excludeUserId);
            if (studentCount != null && studentCount > 0) return true;

            Integer teacherCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM teacher WHERE teacher_email = ? AND user_id != ?",
                    Integer.class, email, excludeUserId);
            if (teacherCount != null && teacherCount > 0) return true;

            Integer adminCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM admin WHERE admin_email = ? AND user_id != ?",
                    Integer.class, email, excludeUserId);
            return adminCount != null && adminCount > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
