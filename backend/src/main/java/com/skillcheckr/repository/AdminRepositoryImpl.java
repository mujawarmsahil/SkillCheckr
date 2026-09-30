package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

@Repository
public class AdminRepositoryImpl implements AdminRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<RegistrationRequest> findRequestById(int requestId) {
        List<RegistrationRequest> requests = jdbcTemplate.query(
                "SELECT request_id, name, email, username, password, contact, requested_role, status "
                        + "FROM request WHERE request_id = ?",
                (rs, rowNum) -> {
                    RegistrationRequest req = new RegistrationRequest();
                    req.setRequestId(rs.getInt("request_id"));
                    req.setName(rs.getString("name"));
                    req.setEmail(rs.getString("email"));
                    req.setUsername(rs.getString("username"));
                    req.setPassword(rs.getString("password"));
                    req.setContact(rs.getString("contact"));
                    req.setRequestedRole(rs.getString("requested_role"));
                    req.setStatus(rs.getString("status"));
                    return req;
                },
                requestId);
        return requests.stream().findFirst();
    }

    @Override
    public boolean existsUserByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)", Integer.class, username);
        return count != null && count > 0;
    }

    @Override
    public boolean existsTeacherByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teacher WHERE LOWER(email) = LOWER(?)", Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public boolean existsStudentByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student WHERE LOWER(email) = LOWER(?)", Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public int createUser(String username, String encodedPassword, String role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO user (username, password, user_role) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, encodedPassword);
            statement.setString(3, role);
            return statement;
        }, keyHolder);
        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("Unable to create the account for this request");
        }
        return generatedKey.intValue();
    }

    @Override
    public void createTeacher(int userId, String name, String contact, String email) {
        jdbcTemplate.update("INSERT INTO teacher (user_id, name, contact, email) VALUES (?, ?, ?, ?)",
                userId, name, contact, email);
    }

    @Override
    public void createStudent(int userId, String name, String contact, String email) {
        jdbcTemplate.update("INSERT INTO student (user_id, name, contact, email) VALUES (?, ?, ?, ?)",
                userId, name, contact, email);
    }

    @Override
    public boolean updateRequestStatus(int requestId, String status) {
        int rows = jdbcTemplate.update("UPDATE request SET status = ? WHERE request_id = ?", status, requestId);
        return rows > 0;
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
                teacher.setProfileImage(rs.getString("profile_image"));
                teacher.setStatus(readStatus(rs));
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
                student.setProfileImage(rs.getString("profile_image"));
                student.setStatus(readStatus(rs));
                return student;
            }
        });
    }

    @Override
    public boolean deleteTeacherById(int teacherId) {
        List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM teacher WHERE teacher_id = ?",
                (rs, rowNum) -> rs.getInt("user_id"), teacherId);

        if (count("SELECT COUNT(*) FROM exam WHERE teacher_id = ?", teacherId) > 0) {
            deactivate("teacher", teacherId, userIds);
            return true;
        }

        int teacherDeleted = jdbcTemplate.update("DELETE FROM teacher WHERE teacher_id = ?", teacherId);
        if (!userIds.isEmpty() && teacherDeleted > 0) {
            jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
        }
        return teacherDeleted > 0;
    }

    @Override
    public boolean deleteStudentById(int studentId) {
        List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM student WHERE student_id = ?",
                (rs, rowNum) -> rs.getInt("user_id"), studentId);

        if (hasAcademicHistory(studentId)) {
            deactivate("student", studentId, userIds);
            return true;
        }

        int studentDeleted = jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", studentId);
        if (!userIds.isEmpty() && studentDeleted > 0) {
            jdbcTemplate.update("DELETE FROM user WHERE user_id = ?", userIds.get(0));
        }
        return studentDeleted > 0;
    }

    private boolean hasAcademicHistory(int studentId) {
        return count("SELECT COUNT(*) FROM result WHERE student_id = ?", studentId) > 0
                || count("SELECT COUNT(*) FROM exam_attempt WHERE student_id = ?", studentId) > 0;
    }

    private void deactivate(String table, int profileId, List<Integer> userIds) {
        jdbcTemplate.update("UPDATE " + table + " SET status = 'Inactive' WHERE "
                + (table.equals("student") ? "student_id" : "teacher_id") + " = ?", profileId);
        if (!userIds.isEmpty()) {
            jdbcTemplate.update("UPDATE user SET status = 'Inactive' WHERE user_id = ?", userIds.get(0));
        }
    }

    private int count(String sql, int id) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return result == null ? 0 : result;
    }

    private static String readStatus(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return status == null || status.trim().isEmpty() ? "Active" : status;
    }

    @Override
    public boolean toggleTeacherStatus(int teacherId, String status) {
        List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM teacher WHERE teacher_id = ?",
                (rs, rowNum) -> rs.getInt("user_id"), teacherId);
        int rows = jdbcTemplate.update("UPDATE teacher SET status = ? WHERE teacher_id = ?", status, teacherId);
        if (rows > 0 && !userIds.isEmpty()) {
            jdbcTemplate.update("UPDATE user SET status = ? WHERE user_id = ?", status, userIds.get(0));
        }
        return rows > 0;
    }

    @Override
    public boolean toggleStudentStatus(int studentId, String status) {
        List<Integer> userIds = jdbcTemplate.query("SELECT user_id FROM student WHERE student_id = ?",
                (rs, rowNum) -> rs.getInt("user_id"), studentId);
        int rows = jdbcTemplate.update("UPDATE student SET status = ? WHERE student_id = ?", status, studentId);
        if (rows > 0 && !userIds.isEmpty()) {
            jdbcTemplate.update("UPDATE user SET status = ? WHERE user_id = ?", status, userIds.get(0));
        }
        return rows > 0;
    }

    @Override
    public AdminStatsResponse getAdminStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        Integer totalStudents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
        Integer totalTeachers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher", Integer.class);
        Integer totalExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam", Integer.class);
        Integer pendingExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE status = 'Pending' OR status IS NULL", Integer.class);
        Integer upcomingExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE (status = 'Upcoming' OR status = 'Approved') "
                        + "AND (DATE(exam_date) > CURDATE() OR (DATE(exam_date) = CURDATE() "
                        + "AND (end_time IS NULL OR end_time >= CURTIME())))",
                Integer.class);
        Integer completedExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE status = 'Completed' OR (DATE(exam_date) < CURDATE() "
                        + "OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME()))",
                Integer.class);
        Integer pendingRequests = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM request WHERE status = 'Pending' OR status IS NULL", Integer.class);
        Integer totalSubjects = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subject", Integer.class);
        Integer totalQuestions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question", Integer.class);
        Integer totalResults = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM result", Integer.class);

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
        return stats;
    }
}
