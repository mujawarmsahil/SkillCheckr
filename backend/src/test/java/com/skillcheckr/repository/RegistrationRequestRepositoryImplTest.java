package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.RegistrationRequest;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RegistrationRequestRepositoryImpl repository;

    @Test
    void saveRequest_returnsTrue_whenInsertSucceeds() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        req.setContact("1234567890");
        req.setEmail("sam@test.com");
        req.setRequestedRole("Student");
        req.setUsername("sam");
        req.setPassword("$2a$10$hashedPass");

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        boolean saved = repository.saveRequest(req);

        assertThat(saved).isTrue();
        verify(jdbcTemplate).update(anyString(), eq("Sam"), eq("1234567890"), eq("sam@test.com"),
                eq("Student"), eq("Pending"), eq("sam"), eq("$2a$10$hashedPass"));
    }

    @Test
    void saveRequest_returnsFalse_whenNoRowsInserted() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        req.setPassword("$2a$10$hashedPass");

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        assertThat(repository.saveRequest(req)).isFalse();
    }

    @Test
    void existsByUsername_returnsTrue_whenRequestExists() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM request WHERE LOWER(username) = LOWER(?)", Integer.class, "sam"))
                .thenReturn(1);

        assertThat(repository.existsByUsername("sam")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenFree() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM request WHERE LOWER(username) = LOWER(?)", Integer.class, "free"))
                .thenReturn(0);

        assertThat(repository.existsByUsername("free")).isFalse();
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
