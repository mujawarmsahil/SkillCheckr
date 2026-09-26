package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.mapper.ExamResultRowMapper;

@Repository
public class ResultRepositoryImpl implements ResultRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
				+ "FROM result r "
				+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE r.student_id = ? ORDER BY r.result_id DESC";
		return jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, studentId);
	}

	@Override
	public Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
				+ "FROM result r "
				+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE r.exam_id = ? AND r.student_id = ? ORDER BY r.result_id DESC LIMIT 1";
		List<ExamResultDTO> results = jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, examId, studentId);
		return results.stream().findFirst();
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name, stu.name AS student_name "
				+ "FROM result r "
				+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "LEFT JOIN student stu ON r.student_id = stu.student_id "
				+ "ORDER BY r.result_id DESC";
		return jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE);
	}

	@Override
	public Optional<ExamResultDTO> findByAttemptId(int attemptId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
				+ "FROM result r LEFT JOIN exam e ON e.exam_id = r.exam_id "
				+ "LEFT JOIN subject s ON s.subject_id = e.subject_id "
				+ "WHERE r.attempt_id = ?";
		List<ExamResultDTO> results = jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, attemptId);
		return results.stream().findFirst();
	}

	@Override
	public ExamResultDTO insertSubmissionResult(ExamResultDTO result) {
		String sql = "INSERT INTO result (exam_id, student_id, marks_obtained, total_marks, passing_marks, "
				+ "percentage, status, submitted_at, attempt_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, result.getExamId());
			statement.setInt(2, result.getStudentId());
			statement.setInt(3, result.getMarksObtained());
			statement.setInt(4, result.getTotalMarks());
			statement.setInt(5, result.getPassingMarks());
			statement.setDouble(6, result.getPercentage());
			statement.setString(7, result.getStatus());
			statement.setTimestamp(8, java.sql.Timestamp.valueOf(result.getSubmittedAt()));
			statement.setInt(9, result.getAttemptId());
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key != null) result.setResultId(key.intValue());
		return result;
	}

	@Override
	public int createSubmittedAttempt(int examId, int studentId) {
		String sql = "INSERT INTO exam_attempt (exam_id, student_id, started_at, expires_at, submitted_at, status) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		LocalDateTime now = LocalDateTime.now();
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, examId);
			statement.setInt(2, studentId);
			statement.setObject(3, now);
			statement.setObject(4, now);
			statement.setObject(5, now);
			statement.setString(6, ExamConstants.ATTEMPT_STATUS_SUBMITTED);
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		return key == null ? 0 : key.intValue();
	}
}