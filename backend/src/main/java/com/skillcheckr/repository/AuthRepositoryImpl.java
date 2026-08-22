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
                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                    matches = passwordEncoder.matches(password, storedPassword);
                } else {
                    matches = storedPassword.equals(password);
                    // Automatically upgrade plain text password to BCrypt hash in database
                    if (matches) {
                        try {
                            String newHash = passwordEncoder.encode(password);
                            jdbcTemplate.update("UPDATE user SET password = ? WHERE user_id = ?", newHash, user.getUserId());
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
}
