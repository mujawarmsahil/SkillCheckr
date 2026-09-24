package com.skillcheckr.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.RegistrationRequest;

@Repository
public class RegistrationRequestRepositoryImpl implements RegistrationRequestRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean saveRequest(RegistrationRequest request) {
        int result = 0;
        String sql = "INSERT INTO Request (name, contact, email, requested_role, status, username, password) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getName());
            ps.setString(2, request.getContact());
            ps.setString(3, request.getEmail());
            ps.setString(4, request.getRequestedRole());
            ps.setString(5, "Pending");
            ps.setString(6, request.getUsername());
            // Hash password with BCrypt before storing
            String rawPassword = request.getPassword();
            String hashedPassword = (rawPassword != null && !rawPassword.startsWith("$2a$"))
                ? passwordEncoder.encode(rawPassword)
                : rawPassword;
            ps.setString(7, hashedPassword);

            result = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result > 0;
    }

    @Override
    public List<RegistrationRequest> getAllRequests() {
        return jdbcTemplate.query("SELECT * FROM Request ORDER BY request_id DESC", new RowMapper<RegistrationRequest>() {
            @Override
            public RegistrationRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
                RegistrationRequest rqm = new RegistrationRequest();
                rqm.setRequestId(rs.getInt("request_id"));
                rqm.setName(rs.getString("name"));
                rqm.setContact(rs.getString("contact"));
                rqm.setEmail(rs.getString("email"));
                rqm.setUsername(rs.getString("username"));
                rqm.setRequestedRole(rs.getString("requested_role"));
                rqm.setStatus(rs.getString("status"));
                return rqm;
            }
        });
    }

    @Override
    public boolean deleteRequestById(int id) {
        int value = jdbcTemplate.update("DELETE FROM request WHERE request_id = ?", id);
        return value > 0;
    }

    @Override
    public boolean updateRequestStatus(int id, String status) {
        int value = jdbcTemplate.update("UPDATE request SET status = ? WHERE request_id = ?", status, id);
        return value > 0;
    }
}
