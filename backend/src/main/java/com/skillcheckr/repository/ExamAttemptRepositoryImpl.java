package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.repository.mapper.ExamAttemptRowMapper;

@Repository
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExamAttemptRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExamAttempt> findByExamIdAndStudentId(int examId, int studentId) {
        String sql = "SELECT attempt_id, exam_id, student_id, started_at, expires_at, submitted_at, status "
                + "FROM exam_attempt WHERE exam_id = ? AND student_id = ? ORDER BY attempt_id DESC";
        return jdbcTemplate.query(sql, ExamAttemptRowMapper.INSTANCE, examId, studentId);
    }

    @Override
    public ExamAttempt findById(int attemptId) {
        String sql = "SELECT attempt_id, exam_id, student_id, started_at, expires_at, submitted_at, status "
                + "FROM exam_attempt WHERE attempt_id = ?";
        List<ExamAttempt> attempts = jdbcTemplate.query(sql, ExamAttemptRowMapper.INSTANCE, attemptId);
        return attempts.isEmpty() ? null : attempts.get(0);
    }

    @Override
    public int createAttempt(ExamAttempt attempt) {
        String sql = "INSERT INTO exam_attempt (exam_id, student_id, started_at, expires_at, submitted_at, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, attempt.getExam() != null ? attempt.getExam().getExamId() : 0);
            statement.setInt(2, attempt.getStudent() != null ? attempt.getStudent().getStudentId() : 0);
            statement.setTimestamp(3, Timestamp.valueOf(attempt.getStartedAt()));
            statement.setTimestamp(4, Timestamp.valueOf(attempt.getExpiresAt()));
            if (attempt.getSubmittedAt() == null) statement.setTimestamp(5, null);
            else statement.setTimestamp(5, Timestamp.valueOf(attempt.getSubmittedAt()));
            statement.setString(6, attempt.getStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.intValue();
    }

    @Override
    public ExamAttempt findByIdForUpdate(int attemptId) {
        String sql = "SELECT attempt_id, exam_id, student_id, started_at, expires_at, submitted_at, status "
                + "FROM exam_attempt WHERE attempt_id = ? FOR UPDATE";
        List<ExamAttempt> attempts = jdbcTemplate.query(sql, ExamAttemptRowMapper.INSTANCE, attemptId);
        return attempts.isEmpty() ? null : attempts.get(0);
    }

    @Override
    public boolean finalizeAttempt(int attemptId, LocalDateTime submittedAt) {
        return jdbcTemplate.update("UPDATE exam_attempt SET status = ?, submitted_at = ? "
                + "WHERE attempt_id = ? AND status = ?",
                ExamConstants.ATTEMPT_STATUS_SUBMITTED,
                Timestamp.valueOf(submittedAt),
                attemptId,
                ExamConstants.ATTEMPT_STATUS_IN_PROGRESS) > 0;
    }
}
