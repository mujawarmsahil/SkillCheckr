package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.constant.ExamConstants;

@ExtendWith(MockitoExtension.class)
class ExamAttemptRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExamAttemptRepositoryImpl repository;

    @Test
    void finalizeAttemptOnlyTransitionsAnInProgressAttempt() throws Exception {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 17, 12, 30);
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        assertThat(repository.finalizeAttempt(31, submittedAt)).isTrue();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> values = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sql.capture(), values.capture());
        assertThat(sql.getValue()).contains("WHERE attempt_id = ? AND status = ?");
        PreparedStatement statement = org.mockito.Mockito.mock(PreparedStatement.class);
        values.getValue().setValues(statement);
        verify(statement).setString(1, ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        verify(statement).setObject(2, submittedAt);
        verify(statement).setInt(3, 31);
        verify(statement).setString(4, ExamConstants.ATTEMPT_STATUS_IN_PROGRESS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void finalizerSweepFiltersByStatusAndUsesAttemptIdKeyset() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());

        assertThat(repository.findInProgressAttemptsAfterId(12, 250)).isEmpty();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), parameters.capture());
        assertThat(sql.getValue()).contains("WHERE a.status = ? AND a.attempt_id > ? ORDER BY a.attempt_id LIMIT ?");
        assertThat(parameters.getValue()).containsExactly(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS, 12, 250);
    }
}
