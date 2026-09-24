package com.skillcheckr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Subject;

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
	public Subject saveExam(Exam exam) {
		try {
			if (exam.getDate() == null || exam.getDate().isEmpty()) {
				System.err.println("Error: Exam date is null or empty.");
				return null;
			}

			if (exam.getStartTime() == null || exam.getEndTime() == null) {
				System.err.println("Error: Start time or End time is null.");
				return null;
			}

			String subjectCode = exam.getSubject() != null ? exam.getSubject().getSubjectCode() : "GEN101";
			String subjectName = exam.getSubject() != null ? exam.getSubject().getSubjectName() : "General";
			int subjectId;

			// Step 1: Check if subject exists
			String checkSubjectQuery = "SELECT COUNT(*) FROM subject WHERE subject_code = ?";
			Integer count = jdbcTemplate.queryForObject(checkSubjectQuery, Integer.class, subjectCode);

			if (count != null && count > 0) {
				String getSubjectIdQuery = "SELECT subject_id FROM subject WHERE subject_code = ? LIMIT 1";
				subjectId = jdbcTemplate.queryForObject(getSubjectIdQuery, Integer.class, subjectCode);
			} else {
				String insertSubjectQuery = "INSERT INTO subject (subject_name, subject_code) VALUES (?, ?)";
				jdbcTemplate.update(insertSubjectQuery, subjectName, subjectCode);

				String getSubjectIdQuery = "SELECT subject_id FROM subject WHERE subject_code = ? LIMIT 1";
				subjectId = jdbcTemplate.queryForObject(getSubjectIdQuery, Integer.class, subjectCode);
			}

			// Step 2: Validate teacher ID if possible
			int teacherId = exam.getTeacherId();
			if (teacherId > 0) {
				String checkTeacherQuery = "SELECT COUNT(*) FROM teacher WHERE teacher_id = ?";
				Integer teacherCount = jdbcTemplate.queryForObject(checkTeacherQuery, Integer.class, teacherId);
				if (teacherCount == null || teacherCount == 0) {
					// Fallback to first teacher if any exists
					try {
						Integer firstTeacher = jdbcTemplate.queryForObject("SELECT teacher_id FROM teacher LIMIT 1", Integer.class);
						if (firstTeacher != null) teacherId = firstTeacher;
					} catch (Exception ignored) {}
				}
			}

			if (exam.getStatus() == null || exam.getStatus().isEmpty()) {
				exam.setStatus("Pending");
			}

			if (exam.getExamType() == null || exam.getExamType().isEmpty()) {
				exam.setExamType("MCQ");
			}

			Timestamp examDate;
			try {
				examDate = Timestamp.valueOf(LocalDateTime.parse(exam.getDate()));
			} catch (Exception e) {
				examDate = new Timestamp(System.currentTimeMillis());
			}

			String insertExamQuery = "INSERT INTO exam (subject_id, teacher_id, exam_name, exam_type, exam_date, duration_minutes, total_marks, pass_marks, status, start_time, end_time) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			jdbcTemplate.update(insertExamQuery, subjectId, teacherId, exam.getExamName(), exam.getExamType(),
					examDate, exam.getDurationMinutes(), exam.getTotalMarks(), exam.getPassingMarks(),
					exam.getStatus(), exam.getStartTime(), exam.getEndTime());

			Subject subject = new Subject();
			subject.setSubjectId(subjectId);
			subject.setSubjectName(subjectName);
			subject.setSubjectCode(subjectCode);

			return subject;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean deleteExamById(int examId) {
		try {
			String getSubjectIdSql = "SELECT subject_id FROM exam WHERE exam_id = ?";
			List<Integer> subjectIds = jdbcTemplate.query(getSubjectIdSql, (rs, rowNum) -> rs.getInt("subject_id"), examId);

			if (subjectIds.isEmpty()) {
				return false;
			}
			int subjectId = subjectIds.get(0);

			List<Integer> questionIds = jdbcTemplate.query("SELECT question_id FROM question WHERE subject_id = ?",
					(rs, rowNum) -> rs.getInt("question_id"), subjectId);

			for (Integer qid : questionIds) {
				jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", qid);
			}

			jdbcTemplate.update("DELETE FROM question WHERE subject_id = ?", subjectId);
			jdbcTemplate.update("DELETE FROM exam WHERE exam_id = ?", examId);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void syncExamStatuses() {
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
	public List<Exam> viewAllUpcomingExam() {
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
	public List<Exam> viewAllCompletedExam() {
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
	public List<Exam> viewAllExams() {
		syncExamStatuses();
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper());
	}


	@Override
	public Exam getExamById(int examId) {
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.exam_id = ?";
		List<Exam> list = jdbcTemplate.query(query, getExamRowMapper(), examId);
		return list.isEmpty() ? null : list.get(0);
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
	public List<com.skillcheckr.model.ExamRegistration> getRegistrationsByStudentId(int studentId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, e.exam_date, e.start_time, e.end_time, e.duration_minutes, e.total_marks, e.pass_marks, e.status as exam_status, s.subject_id, s.subject_name, s.subject_code "
				+ "FROM exam_registration r "
				+ "JOIN exam e ON r.exam_id = e.exam_id "
				+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
				+ "WHERE r.student_id = ? "
				+ "ORDER BY r.registration_id DESC";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			com.skillcheckr.model.ExamRegistration reg = new com.skillcheckr.model.ExamRegistration();
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
	public List<com.skillcheckr.model.Student> getRegisteredStudentsByExamId(int examId) {
		String sql = "SELECT s.* FROM exam_registration r JOIN student s ON r.student_id = s.student_id WHERE r.exam_id = ? ORDER BY s.student_id ASC";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			com.skillcheckr.model.Student s = new com.skillcheckr.model.Student();
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