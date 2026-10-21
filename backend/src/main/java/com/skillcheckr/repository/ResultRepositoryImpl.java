package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.persistence.entity.ResultEntity;
import com.skillcheckr.persistence.repository.ResultJpaRepository;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.mapper.ExamResultRowMapper;

@Repository
public class ResultRepositoryImpl implements ResultRepository {

	private static final DateTimeFormatter SUBMITTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final JdbcTemplate jdbcTemplate;
	private final ResultJpaRepository resultRepository;

	public ResultRepositoryImpl(JdbcTemplate jdbcTemplate, ResultJpaRepository resultRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.resultRepository = resultRepository;
	}

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
		// submitted_at is a MySQL DATETIME wall-clock value. The DTO carries it as a
		// formatted string because it is part of the JSON contract, so it is parsed back to
		// a LocalDateTime and bound without any JDBC timezone conversion. Parsing happens
		// before the insert so an unusable value is reported before a statement is opened.
		LocalDateTime submittedAt = parseSubmittedAt(result.getSubmittedAt());
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
			statement.setObject(8, submittedAt);
			statement.setInt(9, result.getAttemptId());
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key != null) result.setResultId(key.intValue());
		// The insert does not carry the student's name, so the DTO handed back to the caller
		// would report a null student_name while every later read resolves it. One targeted
		// lookup keeps the immediate submission response consistent with the read paths.
		result.setStudentName(findStudentName(result.getStudentId()));
		return result;
	}

	/**
	 * @return the student's name, or null when the student row no longer exists
	 */
	private String findStudentName(int studentId) {
		List<String> names = jdbcTemplate.query(
				"SELECT name FROM student WHERE student_id = ?",
				(rs, rowNum) -> rs.getString("name"), studentId);
		return names.isEmpty() ? null : names.get(0);
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
				.startedAt(formatDateTime(rs.getObject("started_at", LocalDateTime.class)))
				.submittedAt(formatDateTime(rs.getObject("submitted_at", LocalDateTime.class)))
				.build(), examId);
	}

	/**
	 * Turns the DTO's JSON-facing submitted_at string back into the wall-clock value the
	 * DATETIME column stores. The column is NOT NULL, so a missing or unreadable value is
	 * rejected here rather than surfacing later as a constraint violation.
	 */
	private static LocalDateTime parseSubmittedAt(String submittedAt) {
		if (submittedAt == null || submittedAt.isBlank()) {
			throw new IllegalArgumentException("submitted_at is required");
		}
		try {
			return LocalDateTime.parse(submittedAt, SUBMITTED_AT_FORMAT);
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("submitted_at must use the yyyy-MM-dd HH:mm:ss format");
		}
	}

	private String formatDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.format(SUBMITTED_AT_FORMAT);
	}

	@Override
	public boolean updateResultAfterEvaluation(int attemptId, int marksObtained, int totalMarks, int passingMarks,
			double percentage, String status) {
		return jdbcTemplate.update("UPDATE result SET marks_obtained = ?, total_marks = ?, passing_marks = ?, "
				+ "percentage = ?, status = ? WHERE attempt_id = ?",
				marksObtained, totalMarks, passingMarks, percentage, status, attemptId) > 0;
	}
}
