package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.mapper.ExamAttemptRowMapper;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.persistence.entity.ExamAttemptEntity;
import com.skillcheckr.persistence.repository.ExamAttemptJpaRepository;

@Repository
public class ExamAttemptRepositoryImpl implements ExamAttemptRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ExamAttemptJpaRepository attemptRepository;

    public ExamAttemptRepositoryImpl(JdbcTemplate jdbcTemplate, ExamAttemptJpaRepository attemptRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.attemptRepository = attemptRepository;
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
        return attemptRepository.findById(attemptId).map(this::toAttempt).orElse(null);
    }

    @Override
    public int createAttempt(ExamAttempt attempt) {
        ExamAttemptEntity entity = new ExamAttemptEntity();
        entity.setExamId(attempt.getExam() != null ? attempt.getExam().getExamId() : 0);
        entity.setStudentId(attempt.getStudent() != null ? attempt.getStudent().getStudentId() : 0);
        entity.setStartedAt(attempt.getStartedAt());
        entity.setExpiresAt(attempt.getExpiresAt());
        entity.setSubmittedAt(attempt.getSubmittedAt());
        entity.setStatus(attempt.getStatus());
        ExamAttemptEntity saved = attemptRepository.saveAndFlush(entity);
        return saved.getAttemptId();
    }

    @Override
    public boolean hasAttemptsForExam(int examId) {
        return Boolean.TRUE.equals(attemptRepository.existsForExam(examId));
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
        return attemptRepository.finalizeAttempt(attemptId, ExamConstants.ATTEMPT_STATUS_SUBMITTED,
                submittedAt, ExamConstants.ATTEMPT_STATUS_IN_PROGRESS) > 0;
    }

    private ExamAttempt toAttempt(ExamAttemptEntity e) {
        ExamAttempt a = new ExamAttempt();
        a.setAttemptId(e.getAttemptId());
        a.setStartedAt(e.getStartedAt());
        a.setExpiresAt(e.getExpiresAt());
        a.setSubmittedAt(e.getSubmittedAt());
        a.setStatus(e.getStatus());
        com.skillcheckr.model.Exam ex = new com.skillcheckr.model.Exam();
        ex.setExamId(e.getExamId());
        a.setExam(ex);
        com.skillcheckr.model.Student st = new com.skillcheckr.model.Student();
        st.setStudentId(e.getStudentId());
        a.setStudent(st);
        return a;
    }
}
