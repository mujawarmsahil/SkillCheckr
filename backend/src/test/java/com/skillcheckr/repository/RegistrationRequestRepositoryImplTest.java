package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillcheckr.model.RegistrationRequest;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestRepositoryImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private RegistrationRequestRepositoryImpl repository;

    @Test
    void saveRequest_returnsTrue_whenInsertSucceeds() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        req.setContact("1234567890");
        req.setEmail("sam@test.com");
        req.setRequestedRole("Student");
        req.setUsername("sam");
        req.setPassword("plainPass");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashedPass");
        when(preparedStatement.executeUpdate()).thenReturn(1);

        boolean saved = repository.saveRequest(req);

        assertThat(saved).isTrue();
        verify(preparedStatement).setString(1, "Sam");
        verify(preparedStatement).setString(7, "$2a$10$hashedPass");
    }

    @Test
    void saveRequest_preservesAlreadyHashedPassword() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        req.setPassword("$2a$10$alreadyHashed");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        boolean saved = repository.saveRequest(req);

        assertThat(saved).isTrue();
        verify(preparedStatement).setString(7, "$2a$10$alreadyHashed");
    }

    @Test
    void saveRequest_returnsFalse_whenSqlExceptionThrown() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        when(dataSource.getConnection()).thenThrow(new SQLException("DB error"));

        boolean saved = repository.saveRequest(req);

        assertThat(saved).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllRequests_returnsMappedRequests() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setRequestId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(req));

        List<RegistrationRequest> list = repository.getAllRequests();

        assertThat(list).containsExactly(req);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRequestById_returnsMappedRequest() throws Exception {
        RegistrationRequest req = new RegistrationRequest();
        req.setRequestId(10);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(10))).thenReturn(req);

        RegistrationRequest result = repository.getRequestById(10);

        assertThat(result).isSameAs(req);
    }

    @Test
    void deleteRequestById_returnsTrue_whenRowsUpdated() {
        when(jdbcTemplate.update(anyString(), eq(5))).thenReturn(1);

        assertThat(repository.deleteRequestById(5)).isTrue();
    }

    @Test
    void deleteRequestById_returnsFalse_whenNoRowsUpdated() {
        when(jdbcTemplate.update(anyString(), eq(5))).thenReturn(0);

        assertThat(repository.deleteRequestById(5)).isFalse();
    }

    @Test
    void updateRequestStatus_returnsTrue_whenRowsUpdated() {
        when(jdbcTemplate.update(anyString(), eq("Approved"), eq(5))).thenReturn(1);

        assertThat(repository.updateRequestStatus(5, "Approved")).isTrue();
    }
}
