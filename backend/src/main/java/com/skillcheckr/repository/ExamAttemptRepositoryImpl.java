package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.mapper.ExamAttemptRowMapper;

@Repository
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExamAttemptRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExamAttempt> findByExamIdAndStudentId(int examId, int studentId) {
        String sql = "SELECT attempt_id, exam_id, student_id, started_at, expires_at, submitted_at, status "
                + "FROM exam_attempt WHERE exam_id = ? AND student_id = ? ORDER BY attempt_id DESC FOR UPDATE";
        return jdbcTemplate.query(sql, ExamAttemptRowMapper.INSTANCE, examId, studentId);
    }

    @Override
    public List<ExamAttempt> findInProgressAttemptsAfterId(int afterAttemptId, int limit) {
        String sql = "SELECT a.attempt_id, a.exam_id, a.student_id, a.started_at, a.expires_at, "
                + "a.submitted_at, a.status, e.exam_date, e.start_time, e.end_time "
                + "FROM exam_attempt a JOIN exam e ON e.exam_id = a.exam_id "
                + "WHERE a.status = ? AND a.attempt_id > ? ORDER BY a.attempt_id LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ExamAttempt attempt = ExamAttemptRowMapper.INSTANCE.mapRow(rs, rowNum);
            attempt.getExam().setDate(formatExamDate(rs.getObject("exam_date", LocalDateTime.class)));
            java.sql.Time start = rs.getTime("start_time");
            java.sql.Time end = rs.getTime("end_time");
            attempt.getExam().setStartTime(start == null ? null : start.toLocalTime());
            attempt.getExam().setEndTime(end == null ? null : end.toLocalTime());
            return attempt;
        }, ExamConstants.ATTEMPT_STATUS_IN_PROGRESS, afterAttemptId, limit);
    }

    private static String formatExamDate(LocalDateTime examDate) {
        return examDate == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(examDate);
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
            // started_at, expires_at and submitted_at are MySQL DATETIME wall-clock values.
            // setObject(LocalDateTime) stores them verbatim; java.sql.Timestamp would let
            // the driver shift them between the connection timezone and the JVM zone.
            statement.setObject(3, attempt.getStartedAt());
            statement.setObject(4, attempt.getExpiresAt());
            statement.setObject(5, attempt.getSubmittedAt());
            statement.setString(6, attempt.getStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.intValue();
    }

    @Override
    public boolean hasAttemptsForExam(int examId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM exam_attempt WHERE exam_id = ?)",
                Boolean.class, examId);
        return Boolean.TRUE.equals(exists);
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
        PreparedStatementSetter values = statement -> {
            statement.setString(1, ExamConstants.ATTEMPT_STATUS_SUBMITTED);
            // submitted_at is a MySQL DATETIME; bind the wall-clock value without conversion.
            statement.setObject(2, submittedAt);
            statement.setInt(3, attemptId);
            statement.setString(4, ExamConstants.ATTEMPT_STATUS_IN_PROGRESS);
        };
        return jdbcTemplate.update("UPDATE exam_attempt SET status = ?, submitted_at = ? "
                + "WHERE attempt_id = ? AND status = ?", values) > 0;
    }
}
