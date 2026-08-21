package com.skillcheckr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Subject;

@Repository
public class ExamRepositoryImpl implements ExamRepository {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private boolean columnExists(String tableName, String columnName) {
		try {
			String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
			Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private RowMapper<Exam> getExamRowMapper() {
		return new RowMapper<Exam>() {
			@Override
			public Exam mapRow(ResultSet rs, int rowNum) throws SQLException {
				Exam exam = new Exam();
				exam.setExamId(rs.getInt("exam_id"));
				try {
					exam.setTeacherId(rs.getInt("teacher_id"));
				} catch (Exception ignored) {}

				exam.setExamName(rs.getString("exam_name"));
				exam.setDate(rs.getString("exam_date"));
				exam.setStatus(rs.getString("status"));

				Time sqlStartTime = rs.getTime("start_time");
				Time sqlEndTime = rs.getTime("end_time");

				if (sqlStartTime != null)
					exam.setStartTime(sqlStartTime.toLocalTime());
				if (sqlEndTime != null)
					exam.setEndTime(sqlEndTime.toLocalTime());

				try {
					exam.setDurationMinutes(rs.getInt("duration_minutes"));
				} catch (Exception ignored) {}

				try {
					exam.setTotalMarks(rs.getInt("total_marks"));
				} catch (Exception ignored) {}

				try {
					exam.setPassingMarks(rs.getInt("pass_marks"));
				} catch (Exception ignored) {}

				try {
					String examType = rs.getString("exam_type");
					exam.setExamType(examType != null ? examType : "MCQ");
				} catch (Exception e) {
					exam.setExamType("MCQ");
				}

				try {
					int subjectId = rs.getInt("subject_id");
					Subject sub = new Subject();
					sub.setSubjectId(subjectId);
					try {
						sub.setSubjectName(rs.getString("subject_name"));
						sub.setSubjectCode(rs.getString("subject_code"));
					} catch (Exception e) {
						// Fallback query if not joined
						try {
							String subSql = "SELECT subject_id, subject_name, subject_code FROM subject WHERE subject_id = ?";
							Subject fetched = jdbcTemplate.queryForObject(subSql, (srs, sn) -> {
								Subject s = new Subject();
								s.setSubjectId(srs.getInt("subject_id"));
								s.setSubjectName(srs.getString("subject_name"));
								s.setSubjectCode(srs.getString("subject_code"));
								return s;
							}, subjectId);
							if (fetched != null) {
								sub = fetched;
							}
						} catch (Exception ignored) {}
					}
					exam.setSubject(sub);
				} catch (Exception ignored) {}

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
					// Fallback to first teacher or 1 if any exists
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

			// Check if exam_type column exists, if not attempt to add it
			boolean hasExamType = columnExists("exam", "exam_type");
			if (!hasExamType) {
				try {
					jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN exam_type VARCHAR(50) DEFAULT 'MCQ'");
					hasExamType = true;
				} catch (Exception e) {
					System.err.println("Could not alter table exam to add exam_type: " + e.getMessage());
				}
			}

			if (hasExamType) {
				String insertExamQuery = "INSERT INTO exam (subject_id, teacher_id, exam_name, exam_type, exam_date, duration_minutes, total_marks, pass_marks, status, start_time, end_time) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				jdbcTemplate.update(insertExamQuery, subjectId, teacherId, exam.getExamName(), exam.getExamType(),
						examDate, exam.getDurationMinutes(), exam.getTotalMarks(), exam.getPassingMarks(),
						exam.getStatus(), exam.getStartTime(), exam.getEndTime());
			} else {
				String insertExamQuery = "INSERT INTO exam (subject_id, teacher_id, exam_name, exam_date, duration_minutes, total_marks, pass_marks, status, start_time, end_time) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				jdbcTemplate.update(insertExamQuery, subjectId, teacherId, exam.getExamName(),
						examDate, exam.getDurationMinutes(), exam.getTotalMarks(), exam.getPassingMarks(),
						exam.getStatus(), exam.getStartTime(), exam.getEndTime());
			}

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

	@Override
	public boolean acceptExam(int examId) {
		String sql = "UPDATE exam SET status = 'Upcoming' WHERE exam_id = ?";
		int rowsAffected = jdbcTemplate.update(sql, examId);
		return rowsAffected > 0;
	}

	@Override
	public List<Exam> viewAllUpcomingExam() {
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.status = 'Upcoming' OR e.status = 'Approved' ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper());
	}

	@Override
	public List<Exam> viewAllCompletedExam() {
		try {
			String updateQuery = "UPDATE exam SET status = 'Completed' WHERE DATE(exam_date) < CURDATE() AND status = 'Upcoming'";
			jdbcTemplate.update(updateQuery);
		} catch (Exception ignored) {}

		String selectQuery = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.status = 'Completed' ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(selectQuery, getExamRowMapper());
	}

	@Override
	public List<Exam> viewAllExams() {
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper());
	}

	@Override
	public Exam getExamById(int examId) {
		try {
			String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.exam_id = ?";
			List<Exam> list = jdbcTemplate.query(query, getExamRowMapper(), examId);
			return list.isEmpty() ? null : list.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<Exam> getExamsByTeacherId(int teacherId) {
		String query = "SELECT e.*, s.subject_name, s.subject_code FROM exam e LEFT JOIN subject s ON e.subject_id = s.subject_id WHERE e.teacher_id = ? ORDER BY e.exam_id DESC";
		return jdbcTemplate.query(query, getExamRowMapper(), teacherId);
	}
}
