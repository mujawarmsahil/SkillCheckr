package com.skillcheckr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.RegistrationRequest;

@Repository
public class RegistrationRequestRepositoryImpl implements RegistrationRequestRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean saveRequest(RegistrationRequest request) {
        int result = jdbcTemplate.update(
                "INSERT INTO request (name, contact, email, requested_role, status, username, password) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                request.getName(), request.getContact(), request.getEmail(), request.getRequestedRole(),
                "Pending", request.getUsername(), request.getPassword());
        return result > 0;
    }

    @Override
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM request WHERE LOWER(username) = LOWER(?)", Integer.class, username);
        return count != null && count > 0;
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
        int deleted = jdbcTemplate.update("DELETE FROM request WHERE request_id = ?", id);
        return deleted > 0;
    }

    @Override
    public boolean updateRequestStatus(int id, String status) {
        int updated = jdbcTemplate.update("UPDATE request SET status = ? WHERE request_id = ?", status, id);
        return updated > 0;
    }

    @Override
    public boolean updateRequestStatusIfCurrent(int id, String status, String expectedStatus) {
        int updated = jdbcTemplate.update("UPDATE request SET status = ? WHERE request_id = ? AND status = ?",
                status, id, expectedStatus);
        return updated > 0;
    }

    @Override
    public Optional<String> getRequestStatus(int id) {
        return jdbcTemplate.query("SELECT status FROM request WHERE request_id = ?",
                (rs, rowNum) -> rs.getString("status"), id)
                .stream()
                .findFirst();
    }
}
