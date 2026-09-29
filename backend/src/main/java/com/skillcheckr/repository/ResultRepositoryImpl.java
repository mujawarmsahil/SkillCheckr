package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.mapper.ExamResultRowMapper;

@Repository
public class ResultRepositoryImpl implements ResultRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name, stu.name AS student_name "
				+ "FROM result r "
				+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "LEFT JOIN student stu ON r.student_id = stu.student_id "
				+ "WHERE r.student_id = ? ORDER BY r.result_id DESC";
		return jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, studentId);
	}

	@Override
	public Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name, stu.name AS student_name "
				+ "FROM result r "
				+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "LEFT JOIN student stu ON r.student_id = stu.student_id "
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
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name, stu.name AS student_name "
				+ "FROM result r LEFT JOIN exam e ON e.exam_id = r.exam_id "
				+ "LEFT JOIN subject s ON s.subject_id = e.subject_id "
				+ "LEFT JOIN student stu ON r.student_id = stu.student_id "
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
	public List<ExamSubmissionSummaryDTO> findSubmissionSummariesByExamId(int examId) {
		String sql = "SELECT a.attempt_id, a.student_id, a.status AS attempt_status, a.started_at, a.submitted_at, "
				+ "s.name AS student_name, s.email AS student_email, "
				+ "r.result_id, r.marks_obtained, r.total_marks, r.passing_marks, r.percentage, r.status AS result_status "
				+ "FROM exam_attempt a "
				+ "JOIN student s ON s.student_id = a.student_id "
				+ "LEFT JOIN result r ON r.attempt_id = a.attempt_id "
				+ "WHERE a.exam_id = ? ORDER BY a.attempt_id DESC";
		return jdbcTemplate.query(sql, (rs, rowNum) -> ExamSubmissionSummaryDTO.builder()
				.attemptId(rs.getInt("attempt_id"))
				.studentId(rs.getInt("student_id"))
				.studentName(rs.getString("student_name"))
				.studentEmail(rs.getString("student_email"))
				.attemptStatus(rs.getString("attempt_status"))
				.resultStatus(rs.getString("result_status"))
				.marksObtained(rs.getInt("marks_obtained"))
				.totalMarks(rs.getInt("total_marks"))
				.passingMarks(rs.getInt("passing_marks"))
				.percentage(rs.getDouble("percentage"))
				.startedAt(formatDateTime(rs.getTimestamp("started_at")))
				.submittedAt(formatDateTime(rs.getTimestamp("submitted_at")))
				.build(), examId);
	}

	private String formatDateTime(java.sql.Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	@Override
	public boolean updateResultAfterEvaluation(int attemptId, int marksObtained, int totalMarks, int passingMarks,
			double percentage, String status) {
		return jdbcTemplate.update("UPDATE result SET marks_obtained = ?, total_marks = ?, passing_marks = ?, "
				+ "percentage = ?, status = ? WHERE attempt_id = ?",
				marksObtained, totalMarks, passingMarks, percentage, status, attemptId) > 0;
	}
}