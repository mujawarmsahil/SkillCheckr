package com.skillcheckr.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AdminRepositoryImpl implements AdminRepository {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public boolean addStudentFromRequest(int requestId) {
		try (Connection conn = dataSource.getConnection()) {
			String selectSql = "SELECT * FROM request WHERE request_id = ?";
			PreparedStatement psSelect = conn.prepareStatement(selectSql);
			psSelect.setInt(1, requestId);
			ResultSet rs = psSelect.executeQuery();
			if (!rs.next()) return false;

			String username = rs.getString("username");
			String password = rs.getString("password");
			String role = "Student";
			String name = rs.getString("name");
			String contact = rs.getString("contact");
			String email = rs.getString("email");

			// Ensure BCrypt hashed password
			String encodedPassword = (password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")))
				? password
				: passwordEncoder.encode(password);

			String checkUserSql = "SELECT user_id FROM user WHERE username = ?";
			PreparedStatement psCheck = conn.prepareStatement(checkUserSql);
			psCheck.setString(1, username);
			ResultSet checkResult = psCheck.executeQuery();
			int userId;
			if (checkResult.next()) {
				userId = checkResult.getInt("user_id");
			} else {
				String insertUser = "INSERT INTO user (username, password, user_role) VALUES (?, ?, ?)";
				PreparedStatement psUser = conn.prepareStatement(insertUser, PreparedStatement.RETURN_GENERATED_KEYS);
				psUser.setString(1, username);
				psUser.setString(2, encodedPassword);
				psUser.setString(3, role);
				psUser.executeUpdate();
				ResultSet genKeys = psUser.getGeneratedKeys();
				if (!genKeys.next()) return false;
				userId = genKeys.getInt(1);
			}

			String checkStudentSql = "SELECT student_id FROM student WHERE user_id = ? OR email = ?";
			PreparedStatement psCheckStudent = conn.prepareStatement(checkStudentSql);
			psCheckStudent.setInt(1, userId);
			psCheckStudent.setString(2, email);
			ResultSet checkStudentRs = psCheckStudent.executeQuery();
			if (!checkStudentRs.next()) {
				String insertStudent = "INSERT INTO student(user_id, name, contact, email) VALUES (?, ?, ?, ?)";
				PreparedStatement psStudent = conn.prepareStatement(insertStudent);
				psStudent.setInt(1, userId);
				psStudent.setString(2, name);
				psStudent.setString(3, contact);
				psStudent.setString(4, email);
				psStudent.executeUpdate();
			}

			String updateRequest = "UPDATE request SET status = 'Approved' WHERE request_id = ?";
			PreparedStatement psUpdate = conn.prepareStatement(updateRequest);
			psUpdate.setInt(1, requestId);
			psUpdate.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("Error adding student from request", e);
			return false;
		}
	}

	@Override
	public boolean addTeacherFromRequest(int requestId) {
		try (Connection conn = dataSource.getConnection()) {
			String selectSql = "SELECT * FROM request WHERE request_id = ?";
			PreparedStatement psSelect = conn.prepareStatement(selectSql);
			psSelect.setInt(1, requestId);
			ResultSet rs = psSelect.executeQuery();
			if (!rs.next()) return false;

			String username = rs.getString("username");
			String password = rs.getString("password");
			String role = "Teacher";
			String name = rs.getString("name");
			String contact = rs.getString("contact");
			String email = rs.getString("email");

			// Ensure BCrypt hashed password
			String encodedPassword = (password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")))
				? password
				: passwordEncoder.encode(password);

			String checkUserSql = "SELECT user_id FROM user WHERE username = ?";
			PreparedStatement psCheck = conn.prepareStatement(checkUserSql);
			psCheck.setString(1, username);
			ResultSet checkResult = psCheck.executeQuery();
			int userId;
			if (checkResult.next()) {
				userId = checkResult.getInt("user_id");
			} else {
				String insertUser = "INSERT INTO user (username, password, user_role) VALUES (?, ?, ?)";
				PreparedStatement psUser = conn.prepareStatement(insertUser, PreparedStatement.RETURN_GENERATED_KEYS);
				psUser.setString(1, username);
				psUser.setString(2, encodedPassword);
				psUser.setString(3, role);
				psUser.executeUpdate();
				ResultSet genKeys = psUser.getGeneratedKeys();
				if (!genKeys.next()) return false;
				userId = genKeys.getInt(1);
			}

			String checkTeacherSql = "SELECT teacher_id FROM teacher WHERE user_id = ? OR email = ?";
			PreparedStatement psCheckTeacher = conn.prepareStatement(checkTeacherSql);
			psCheckTeacher.setInt(1, userId);
			psCheckTeacher.setString(2, email);
			ResultSet checkTeacherRs = psCheckTeacher.executeQuery();
			if (!checkTeacherRs.next()) {
				String insertTeacher = "INSERT INTO teacher(user_id, name, contact, email) VALUES (?, ?, ?, ?)";
				PreparedStatement psTeacher = conn.prepareStatement(insertTeacher);
				psTeacher.setInt(1, userId);
				psTeacher.setString(2, name);
				psTeacher.setString(3, contact);
				psTeacher.setString(4, email);
				psTeacher.executeUpdate();
			}

			String updateRequest = "UPDATE request SET status = 'Approved' WHERE request_id = ?";
			PreparedStatement psUpdate = conn.prepareStatement(updateRequest);
			psUpdate.setInt(1, requestId);
			psUpdate.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("Error adding teacher from request", e);
			return false;
		}
	}

	@Override
	public List<Teacher> getAllTeacher() {
		return jdbcTemplate.query("SELECT * FROM teacher ORDER BY teacher_id DESC", new RowMapper<Teacher>() {
			@Override
			public Teacher mapRow(ResultSet rs, int rowNum) throws SQLException {
				Teacher teacher = new Teacher();
				teacher.setTeacherId(rs.getInt("teacher_id"));
				teacher.setUserId(rs.getInt("user_id"));
				teacher.setName(rs.getString("name"));
				teacher.setEmail(rs.getString("email"));
				teacher.setContact(rs.getString("contact"));
				try {
					teacher.setProfileImage(rs.getString("profile_image"));
				} catch (Exception ignored) {}
				try {
					String st = rs.getString("status");
					teacher.setStatus(st != null && !st.trim().isEmpty() ? st : "Active");
				} catch (Exception ignored) {
					teacher.setStatus("Active");
				}
				return teacher;
			}
		});
	}

	@Override
	public List<Student> getAllStudent() {
		return jdbcTemplate.query("SELECT * FROM student ORDER BY student_id DESC", new RowMapper<Student>() {
			@Override
			public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
				Student student = new Student();
				student.setStudentId(rs.getInt("student_id"));
				student.setUserId(rs.getInt("user_id"));
				student.setName(rs.getString("name"));
				student.setEmail(rs.getString("email"));
				student.setContact(rs.getString("contact"));
				try {
					student.setProfileImage(rs.getString("profile_image"));
				} catch (Exception ignored) {}
				try {
					String st = rs.getString("status");
					student.setStatus(st != null && !st.trim().isEmpty() ? st : "Active");
				} catch (Exception ignored) {
					student.setStatus("Active");
				}
				return student;
			}
		});
	}

	@Override
	public boolean deleteTeacherById(int teacherId) {
		try {
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM teacher WHERE teacher_id = ?",
				(rs, rowNum) -> rs.getInt("user_id"), teacherId);

			Integer examCount = 0;
			try {
				examCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE teacher_id = ?", Integer.class, teacherId);
			} catch (Exception ignored) {}

			if (examCount != null && examCount > 0) {
				// Prefer soft deletion / deactivation to preserve exam history
				jdbcTemplate.update("UPDATE teacher SET status = 'Inactive' WHERE teacher_id = ?", teacherId);
				if (!userIds.isEmpty()) {
					try {
						jdbcTemplate.update("UPDATE user SET status = 'Inactive' WHERE user_id = ?", userIds.get(0));
					} catch (Exception ignored) {}
				}
				return true;
			}

			// Safe to hard delete if no exams created
			int teacherDeleted = jdbcTemplate.update("DELETE FROM teacher WHERE teacher_id = ?", teacherId);
			if (!userIds.isEmpty() && teacherDeleted > 0) {
				jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
			}
			return teacherDeleted > 0;
		} catch (Exception e) {
			log.error("Error deleting teacher", e);
			return false;
		}
	}

	@Override
	public boolean deleteStudentById(int studentId) {
		try {
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM student WHERE student_id = ?",
				(rs, rowNum) -> rs.getInt("user_id"), studentId);

			Integer resultCount = 0;
			try {
				resultCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM result WHERE student_id = ?", Integer.class, studentId);
			} catch (Exception ignored) {}

			if (resultCount != null && resultCount > 0) {
				// Prefer soft deactivation to preserve academic history & scorecard integrity
				jdbcTemplate.update("UPDATE student SET status = 'Inactive' WHERE student_id = ?", studentId);
				if (!userIds.isEmpty()) {
					try {
						jdbcTemplate.update("UPDATE user SET status = 'Inactive' WHERE user_id = ?", userIds.get(0));
					} catch (Exception ignored) {}
				}
				return true;
			}

			try {
				jdbcTemplate.update("DELETE FROM exam_registration WHERE student_id = ?", studentId);
			} catch (Exception ignored) {}

			int studentDeleted = jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", studentId);
			if (!userIds.isEmpty() && studentDeleted > 0) {
				jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
			}
			return studentDeleted > 0;
		} catch (Exception e) {
			log.error("Error deleting student", e);
			return false;
		}
	}

	@Override
	public boolean toggleTeacherStatus(int teacherId, String status) {
		try {
			String safeStatus = (status != null && status.equalsIgnoreCase("Inactive")) ? "Inactive" : "Active";
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM teacher WHERE teacher_id = ?",
					(rs, rowNum) -> rs.getInt("user_id"), teacherId);
			int rows = jdbcTemplate.update("UPDATE teacher SET status = ? WHERE teacher_id = ?", safeStatus, teacherId);
			if (!userIds.isEmpty()) {
				try {
					jdbcTemplate.update("UPDATE user SET status = ? WHERE user_id = ?", safeStatus, userIds.get(0));
				} catch (Exception ignored) {}
			}
			return rows > 0;
		} catch (Exception e) {
			log.error("Error toggling teacher status", e);
			return false;
		}
	}

	@Override
	public boolean toggleStudentStatus(int studentId, String status) {
		try {
			String safeStatus = (status != null && status.equalsIgnoreCase("Inactive")) ? "Inactive" : "Active";
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM student WHERE student_id = ?",
					(rs, rowNum) -> rs.getInt("user_id"), studentId);
			int rows = jdbcTemplate.update("UPDATE student SET status = ? WHERE student_id = ?", safeStatus, studentId);
			if (!userIds.isEmpty()) {
				try {
					jdbcTemplate.update("UPDATE user SET status = ? WHERE user_id = ?", safeStatus, userIds.get(0));
				} catch (Exception ignored) {}
			}
			return rows > 0;
		} catch (Exception e) {
			log.error("Error toggling student status", e);
			return false;
		}
	}

	@Override
	public AdminStatsResponse getAdminStats() {
		AdminStatsResponse stats = new AdminStatsResponse();
		try {
			try {
				String syncSql = "UPDATE exam SET status = 'Completed' "
						+ "WHERE (status = 'Upcoming' OR status = 'Approved') "
						+ "AND ("
						+ "  DATE(exam_date) < CURDATE() "
						+ "  OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME())"
						+ ")";
				jdbcTemplate.update(syncSql);
			} catch (Exception ignored) {}

			Integer totalStudents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
			Integer totalTeachers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher", Integer.class);
			Integer totalExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam", Integer.class);
			Integer pendingExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE status = 'Pending' OR status IS NULL", Integer.class);
			Integer upcomingExams = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM exam WHERE (status = 'Upcoming' OR status = 'Approved') AND (DATE(exam_date) > CURDATE() OR (DATE(exam_date) = CURDATE() AND (end_time IS NULL OR end_time >= CURTIME())))",
					Integer.class);
			Integer completedExams = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM exam WHERE status = 'Completed' OR (DATE(exam_date) < CURDATE() OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME()))",
					Integer.class);
			Integer pendingRequests = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM request WHERE status = 'Pending' OR status IS NULL", Integer.class);

			Integer totalSubjects = 0;
			try {
				totalSubjects = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subject", Integer.class);
			} catch (Exception ignored) {}

			Integer totalQuestions = 0;
			try {
				totalQuestions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question", Integer.class);
			} catch (Exception ignored) {}

			Integer totalResults = 0;
			try {
				totalResults = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM result", Integer.class);
			} catch (Exception ignored) {}

			stats.setTotalStudents(totalStudents != null ? totalStudents : 0);
			stats.setTotalTeachers(totalTeachers != null ? totalTeachers : 0);
			stats.setTotalExams(totalExams != null ? totalExams : 0);
			stats.setPendingExams(pendingExams != null ? pendingExams : 0);
			stats.setUpcomingExams(upcomingExams != null ? upcomingExams : 0);
			stats.setCompletedExams(completedExams != null ? completedExams : 0);
			stats.setPendingRequests(pendingRequests != null ? pendingRequests : 0);
			stats.setTotalSubjects(totalSubjects != null ? totalSubjects : 0);
			stats.setTotalQuestions(totalQuestions != null ? totalQuestions : 0);
			stats.setTotalResults(totalResults != null ? totalResults : 0);
		} catch (Exception e) {
			log.error("Error gathering admin stats", e);
		}
		return stats;
	}

}
