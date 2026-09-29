package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ExamRepositoryImpl implements ExamRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private RowMapper<Exam> getExamRowMapper() {
		return new RowMapper<Exam>() {
			@Override
			public Exam mapRow(ResultSet rs, int rowNum) throws SQLException {
				Exam exam = new Exam();
				exam.setExamId(rs.getInt("exam_id"));
				exam.setTeacherId(rs.getInt("teacher_id"));
				exam.setExamName(rs.getString("exam_name"));
				exam.setDate(rs.getString("exam_date"));
				exam.setStatus(rs.getString("status"));

				Time sqlStartTime = rs.getTime("start_time");
				Time sqlEndTime = rs.getTime("end_time");

				if (sqlStartTime != null)
					exam.setStartTime(sqlStartTime.toLocalTime());
				if (sqlEndTime != null)
					exam.setEndTime(sqlEndTime.toLocalTime());

				exam.setDurationMinutes(rs.getInt("duration_minutes"));
				exam.setTotalMarks(rs.getInt("total_marks"));
				exam.setPassingMarks(rs.getInt("pass_marks"));

				String examType = rs.getString("exam_type");
				exam.setExamType(examType != null ? examType : "MCQ");

				Subject sub = new Subject();
				sub.setSubjectId(rs.getInt("subject_id"));
				sub.setSubjectName(rs.getString("subject_name"));
				sub.setSubjectCode(rs.getString("subject_code"));
				exam.setSubject(sub);

				return exam;
			}
		};
	}

	@Override
	public Exam saveExam(Exam exam) {
		if (exam.getDate() == null || exam.getDate().isEmpty()) {
			throw new BadRequestException("Exam date is required");
		}
		if (exam.getStartTime() == null || exam.getEndTime() == null) {
			throw new BadRequestException("Exam start and end times are required");
		}

		String subjectCode = exam.getSubject() != null ? exam.getSubject().getSubjectCode() : null;
		if (subjectCode == null || subjectCode.isBlank()) {
			throw new BadRequestException("A valid subject is required");
		}

		Subject subject = findSubjectByCode(subjectCode.trim())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Subject '" + subjectCode.trim() + "' does not exist. Subjects are managed by an administrator."));

		int teacherId = exam.getTeacherId();
		if (teacherId > 0 && !teacherExists(teacherId)) {
			throw new ResourceNotFoundException("Teacher " + teacherId + " does not exist");
		}

		if (exam.getStatus() == null || exam.getStatus().isBlank()) {
			exam.setStatus(ExamConstants.EXAM_STATUS_PENDING);
		}

		if (exam.getExamType() == null || exam.getExamType().isBlank()) {
			exam.setExamType(ExamConstants.QUESTION_TYPE_MCQ);
		}

		Timestamp examDate;
		try {
			examDate = Timestamp.valueOf(parseExamDate(exam.getDate()));
		} catch (DateTimeException ex) {
			throw new BadRequestException("Exam date must be a valid date and time value");
		}

		String insertExamQuery = "INSERT INTO exam (subject_id, teacher_id, exam_name, exam_type, exam_date, duration_minutes, total_marks, pass_marks, status, start_time, end_time) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(insertExamQuery, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, subject.getSubjectId());
			if (teacherId > 0) {
				ps.setInt(2, teacherId);
			} else {
				ps.setNull(2, java.sql.Types.INTEGER);
			}
			ps.setString(3, exam.getExamName());
			ps.setString(4, exam.getExamType());
			ps.setTimestamp(5, examDate);
			ps.setInt(6, exam.getDurationMinutes());
			ps.setInt(7, exam.getTotalMarks());
			ps.setInt(8, exam.getPassingMarks());
			ps.setString(9, exam.getStatus());
			ps.setObject(10, exam.getStartTime());
			ps.setObject(11, exam.getEndTime());
			return ps;
		}, keyHolder);

		Number generatedKey = keyHolder.getKey();
		if (generatedKey == null) {
			throw new IllegalStateException("Unable to add exam.");
		}

		exam.setExamId(generatedKey.intValue());
		exam.setSubject(subject);
		return exam;
	}

	/**
	 * Accepts the date shapes a client may send: an ISO date time, the same value with a space
	 * instead of the "T" separator, and a plain calendar date.
	 */
	private LocalDateTime parseExamDate(String rawExamDate) {
		String value = rawExamDate.trim();
		try {
			return LocalDateTime.parse(value);
		} catch (DateTimeParseException ignored) {
			// fall through to the more forgiving shapes
		}
		try {
			return LocalDateTime.parse(value.replace(' ', 'T'));
		} catch (DateTimeParseException ignored) {
			// fall through to a date only value
		}
		return LocalDate.parse(value.split("[ T]")[0]).atStartOfDay();
	}

	private Optional<Subject> findSubjectByCode(String subjectCode) {
		String sql = "SELECT subject_id, subject_name, subject_code FROM subject WHERE subject_code = ? LIMIT 1";
		List<Subject> subjects = jdbcTemplate.query(sql, (rs, rowNum) -> {
			Subject subject = new Subject();
			subject.setSubjectId(rs.getInt("subject_id"));
			subject.setSubjectName(rs.getString("subject_name"));
			subject.setSubjectCode(rs.getString("subject_code"));
			return subject;
		}, subjectCode);
		return subjects.stream().findFirst();
	}

	private boolean teacherExists(int teacherId) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher WHERE teacher_id = ?",
				Integer.class, teacherId);
		return count != null && count > 0;
	}

	@Override
	public boolean deleteExamById(int examId) {
		if (!examExists(examId)) {
			return false;
		}
		Integer attemptCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM exam_attempt WHERE exam_id = ?", Integer.class, examId);
		if (attemptCount != null && attemptCount > 0) {
			throw new ExamInUseException("This exam already has student attempts and cannot be deleted. "
					+ "Cancel the exam instead to keep the recorded results.");
		}

		// Only the exam and its question assignments belong to this exam. Questions and
		// answers are subject level records shared with other exams and must survive.
		jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id = ?", examId);
		return jdbcTemplate.update("DELETE FROM exam WHERE exam_id = ?", examId) > 0;
	}

	private boolean examExists(int examId) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE exam_id = ?",
				Integer.class, examId);
		return count != null && count > 0;
	}

	@Override
	public void syncExamStatuses() {
		String updateQuery = "UPDATE exam SET status = 'Completed' "
				+ "WHERE (status = 'Upcoming' OR status = 'Approved') "
				+ "AND ("
				+ "  DATE(exam_date) < CURDATE() "
				+ "  OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME())"
				+ ")";
		jdbcTemplate.update(updateQuery);
	}

	@Override
	public boolean acceptExam(int examId) {
		String sql = "UPDATE exam SET status = 'Upcoming' WHERE exam_id = ?";
		int rowsAffected = jdbcTemplate.update(sql, examId);
		return rowsAffected > 0;
	}

	@Override
	public boolean updateExamStatus(int examId, String status) {
		String sql = "UPDATE exam SET status = ? WHERE exam_id = ?";
		int rowsAffected = jdbcTemplate.update(sql, status, examId);
		return rowsAffected > 0;
	}

	@Override
	public List<Exam> getAllUpcomingExams() {
		syncExamStatuses();
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE (e.status = 'Upcoming' OR e.status = 'Approved') "
				+ "AND ("
				+ "  DATE(e.exam_date) > CURDATE() "
				+ "  OR (DATE(e.exam_date) = CURDATE() AND (e.end_time IS NULL OR e.end_time >= CURTIME()))"
				+ ") "
				+ "ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper());
	}

	@Override
	public List<Exam> getAllCompletedExams() {
		syncExamStatuses();
		String selectQuery = "SELECT e.*, s.subject_name, s.subject_code FROM exam e "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE e.status = 'Completed' "
				+ "OR ("
				+ "  (e.status = 'Upcoming' OR e.status = 'Approved') "
				+ "  AND ("
				+ "    DATE(e.exam_date) < CURDATE() "
				+ "    OR (DATE(e.exam_date) = CURDATE() AND e.end_time IS NOT NULL AND e.end_time < CURTIME())"
				+ "  )"
				+ ") "
				+ "ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(selectQuery, getExamRowMapper());
	}

	@Override
	public List<Exam> getAllExams() {
		syncExamStatuses();
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper());
	}


	@Override
	public Optional<Exam> getExamById(int examId) {
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.exam_id = ?";
		List<Exam> exams = jdbcTemplate.query(query, getExamRowMapper(), examId);
		return exams.stream().findFirst();
	}

	@Override
	public List<Exam> getExamsByTeacherId(int teacherId) {
		syncExamStatuses();
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.teacher_id = ? ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper(), teacherId);
	}

	@Override
	public boolean registerStudentForExam(int studentId, int examId) {
		if (isStudentRegisteredForExam(studentId, examId)) {
			return true;
		}

		String sql = "INSERT INTO exam_registration (student_id, exam_id, status) VALUES (?, ?, 'Registered') "
				+ "ON DUPLICATE KEY UPDATE status = 'Registered'";
		int rows = jdbcTemplate.update(sql, studentId, examId);
		return rows > 0;
	}

	@Override
	public boolean isStudentRegisteredForExam(int studentId, int examId) {
		String sql = "SELECT COUNT(*) FROM exam_registration WHERE student_id = ? AND exam_id = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, examId);
		return count != null && count > 0;
	}

	@Override
	public List<Integer> getRegisteredExamIdsForStudent(int studentId) {
		String sql = "SELECT exam_id FROM exam_registration WHERE student_id = ?";
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("exam_id"), studentId);
	}

	@Override
	public List<ExamRegistration> getRegistrationsByStudentId(int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, e.exam_date, e.start_time, e.end_time, e.duration_minutes, e.total_marks, e.pass_marks, e.status as exam_status, s.subject_id, s.subject_name, s.subject_code "
				+ "FROM exam_registration r "
				+ "JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE r.student_id = ? "
				+ "ORDER BY r.registration_id DESC";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ExamRegistration reg = new ExamRegistration();
			reg.setRegistrationId(rs.getInt("registration_id"));
			reg.setStudentId(rs.getInt("student_id"));
			reg.setExamId(rs.getInt("exam_id"));
			reg.setRegisteredAt(rs.getString("registered_at"));
			reg.setStatus(rs.getString("status"));

			Exam exam = new Exam();
			exam.setExamId(rs.getInt("exam_id"));
			exam.setExamName(rs.getString("exam_name"));
			exam.setExamType(rs.getString("exam_type"));
			exam.setDate(rs.getString("exam_date"));
			exam.setStatus(rs.getString("exam_status"));
			Time sqlStartTime = rs.getTime("start_time");
			Time sqlEndTime = rs.getTime("end_time");
			if (sqlStartTime != null) exam.setStartTime(sqlStartTime.toLocalTime());
			if (sqlEndTime != null) exam.setEndTime(sqlEndTime.toLocalTime());
			exam.setDurationMinutes(rs.getInt("duration_minutes"));
			exam.setTotalMarks(rs.getInt("total_marks"));
			exam.setPassingMarks(rs.getInt("pass_marks"));

			Subject sub = new Subject();
			sub.setSubjectId(rs.getInt("subject_id"));
			sub.setSubjectName(rs.getString("subject_name"));
			sub.setSubjectCode(rs.getString("subject_code"));
			exam.setSubject(sub);

			reg.setExam(exam);
			return reg;
		}, studentId);
	}

	@Override
	public List<Student> getRegisteredStudentsByExamId(int examId) {
		String sql = "SELECT s.* FROM exam_registration r JOIN student s ON r.student_id = s.student_id WHERE r.exam_id = ? ORDER BY s.student_id ASC";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Student s = new Student();
			s.setStudentId(rs.getInt("student_id"));
			s.setStudentName(rs.getString("name"));
			s.setStudentContact(rs.getString("contact"));
			s.setStudentEmail(rs.getString("email"));
			return s;
		}, examId);
	}

	@Override
	public int getRegistrationCountByExamId(int examId) {
		String sql = "SELECT COUNT(*) FROM exam_registration WHERE exam_id = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, examId);
		return count != null ? count : 0;
	}

	@Override
	public boolean unregisterStudentFromExam(int studentId, int examId) {
		String sql = "DELETE FROM exam_registration WHERE student_id = ? AND exam_id = ?";
		int rows = jdbcTemplate.update(sql, studentId, examId);
		return rows > 0;
	}
}