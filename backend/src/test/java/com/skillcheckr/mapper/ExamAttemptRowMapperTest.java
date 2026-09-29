package com.skillcheckr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.skillcheckr.model.ExamAttempt;

class ExamAttemptRowMapperTest {

    @Test
    void mapsExamAndStudentReferences() throws SQLException {
        ExamAttempt attempt = ExamAttemptRowMapper.INSTANCE.mapRow(baseResultSet(), 0);

        assertThat(attempt.getAttemptId()).isEqualTo(5);
        assertThat(attempt.getExam()).isNotNull();
        assertThat(attempt.getExam().getExamId()).isEqualTo(3);
        assertThat(attempt.getStudent()).isNotNull();
        assertThat(attempt.getStudent().getStudentId()).isEqualTo(42);
        assertThat(attempt.getStatus()).isEqualTo("InProgress");
    }

    @Test
    void leavesReferencesNullWhenTheIdentifiersAreZero() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getInt("exam_id")).thenReturn(0);
        when(rs.getInt("student_id")).thenReturn(0);

        ExamAttempt attempt = ExamAttemptRowMapper.INSTANCE.mapRow(rs, 0);

        assertThat(attempt.getExam()).isNull();
        assertThat(attempt.getStudent()).isNull();
    }

    @Test
    void mapsTimestamps() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getTimestamp("started_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 9, 25, 10, 0)));
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 9, 25, 12, 0)));

        ExamAttempt attempt = ExamAttemptRowMapper.INSTANCE.mapRow(rs, 0);

        assertThat(attempt.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 10, 0));
        assertThat(attempt.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 9, 25, 12, 0));
        assertThat(attempt.getSubmittedAt()).isNull();
    }

    @Test
    void propagatesFailuresInsteadOfSilentlyDroppingColumns() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getInt("student_id")).thenThrow(new SQLException("connection reset"));

        assertThatThrownBy(() -> ExamAttemptRowMapper.INSTANCE.mapRow(rs, 0))
                .isInstanceOf(SQLException.class)
                .hasMessage("connection reset");
    }

    private ResultSet baseResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("attempt_id")).thenReturn(5);
        when(rs.getInt("exam_id")).thenReturn(3);
        when(rs.getInt("student_id")).thenReturn(42);
        when(rs.getString("status")).thenReturn("InProgress");
        return rs;
    }
}
