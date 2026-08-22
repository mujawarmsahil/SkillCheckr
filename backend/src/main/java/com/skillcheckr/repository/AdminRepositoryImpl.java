package com.skillcheckr.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

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

			// Check if username already exists
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

			// Check if student already exists for this user_id
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

			// Update request status to Approved
			String updateRequest = "UPDATE request SET status = 'Approved' WHERE request_id = ?";
			PreparedStatement psUpdate = conn.prepareStatement(updateRequest);
			psUpdate.setInt(1, requestId);
			psUpdate.executeUpdate();

			return true;
		} catch (Exception e) {
			System.err.println("Error adding student from request: " + e.getMessage());
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

			// Check if username already exists
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

			// Check if teacher already exists
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

			// Update request status to Approved
			String updateRequest = "UPDATE request SET status = 'Approved' WHERE request_id = ?";
			PreparedStatement psUpdate = conn.prepareStatement(updateRequest);
			psUpdate.setInt(1, requestId);
			psUpdate.executeUpdate();

			return true;
		} catch (Exception e) {
			System.err.println("Error adding teacher from request: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean isUsernameExist(String username) {
		try {
			String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
			return count != null && count > 0;
		} catch (Exception e) {
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
				teacher.setTeacherName(rs.getString("name"));
				teacher.setTeacherEmail(rs.getString("email"));
				teacher.setTeacherContact(rs.getString("contact"));
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
				student.setStudentName(rs.getString("name"));
				student.setStudentEmail(rs.getString("email"));
				student.setStudentContact(rs.getString("contact"));
				return student;
			}
		});
	}

	@Override
	public boolean deleteTeacherById(int teacherId) {
		try {
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM teacher WHERE teacher_id = ?",
				(rs, rowNum) -> rs.getInt("user_id"), teacherId);
			int teacherDeleted = jdbcTemplate.update("DELETE FROM teacher WHERE teacher_id = ?", teacherId);
			if (!userIds.isEmpty() && teacherDeleted > 0) {
				jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
			}
			return teacherDeleted > 0;
		} catch (Exception e) {
			System.err.println("Error deleting teacher: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean deleteStudentById(int studentId) {
		try {
			List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM student WHERE student_id = ?",
				(rs, rowNum) -> rs.getInt("user_id"), studentId);
			int studentDeleted = jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", studentId);
			if (!userIds.isEmpty() && studentDeleted > 0) {
				jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
			}
			return studentDeleted > 0;
		} catch (Exception e) {
			System.err.println("Error deleting student: " + e.getMessage());
			return false;
		}
	}

	@Override
	public String getUsernameByRequestId(int requestId) {
		try {
			String sql = "SELECT username FROM request WHERE request_id = ?";
			return jdbcTemplate.queryForObject(sql, String.class, requestId);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Map<String, Object> getAdminStats() {
		Map<String, Object> stats = new HashMap<>();
		try {
			Integer totalStudents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
			Integer totalTeachers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher", Integer.class);
			Integer totalExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam", Integer.class);
			Integer pendingExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE status = 'Pending'", Integer.class);
			Integer upcomingExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE status = 'Upcoming' OR status = 'Approved'", Integer.class);
			Integer completedExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE status = 'Completed'", Integer.class);
			Integer pendingRequests = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM request WHERE status = 'Pending'", Integer.class);

			stats.put("totalStudents", totalStudents != null ? totalStudents : 0);
			stats.put("totalTeachers", totalTeachers != null ? totalTeachers : 0);
			stats.put("totalExams", totalExams != null ? totalExams : 0);
			stats.put("pendingExams", pendingExams != null ? pendingExams : 0);
			stats.put("upcomingExams", upcomingExams != null ? upcomingExams : 0);
			stats.put("completedExams", completedExams != null ? completedExams : 0);
			stats.put("pendingRequests", pendingRequests != null ? pendingRequests : 0);
		} catch (Exception e) {
			System.err.println("Error gathering admin stats: " + e.getMessage());
		}
		return stats;
	}
}
