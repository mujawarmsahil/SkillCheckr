package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

@ExtendWith(MockitoExtension.class)
class AdminRepositoryImplTest {

    private static final String REQUEST_SQL = "SELECT request_id, name, email, username, password, contact, "
            + "requested_role, status "
            + "FROM request WHERE request_id = ?";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AdminRepositoryImpl repository;

    private void stubGeneratedUserId(int userId) {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("user_id", (long) userId));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findRequestById_returnsRequestWhenPresent() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("request_id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Alice");
        when(rs.getString("email")).thenReturn("alice@test.com");
        when(rs.getString("username")).thenReturn("alice");
        when(rs.getString("password")).thenReturn("pass");
        when(rs.getString("contact")).thenReturn("123");
        when(rs.getString("requested_role")).thenReturn("Student");
        when(rs.getString("status")).thenReturn("Pending");

        when(jdbcTemplate.query(eq(REQUEST_SQL), any(RowMapper.class), eq(1)))
                .thenAnswer(inv -> {
                    RowMapper<RegistrationRequest> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<RegistrationRequest> result = repository.findRequestById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(result.get().getRequestedRole()).isEqualTo("Student");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findRequestById_returnsEmptyWhenNotFound() {
        when(jdbcTemplate.query(eq(REQUEST_SQL), any(RowMapper.class), eq(99)))
                .thenReturn(List.of());

        assertThat(repository.findRequestById(99)).isEmpty();
    }

    @Test
    void existsUserByUsername_returnsTrue_whenFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)"),
                eq(Integer.class), eq("alice"))).thenReturn(1);

        assertThat(repository.existsUserByUsername("alice")).isTrue();
    }

    @Test
    void existsUserByUsername_returnsFalse_whenNotFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)"),
                eq(Integer.class), eq("alice"))).thenReturn(0);

        assertThat(repository.existsUserByUsername("alice")).isFalse();
    }

    @Test
    void existsTeacherByEmail_returnsTrue_whenFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM teacher WHERE LOWER(email) = LOWER(?)"),
                eq(Integer.class), eq("alice@test.com"))).thenReturn(1);

        assertThat(repository.existsTeacherByEmail("alice@test.com")).isTrue();
    }

    @Test
    void existsStudentByEmail_returnsTrue_whenFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM student WHERE LOWER(email) = LOWER(?)"),
                eq(Integer.class), eq("alice@test.com"))).thenReturn(1);

        assertThat(repository.existsStudentByEmail("alice@test.com")).isTrue();
    }

    @Test
    void createUser_insertsUserAndReturnsGeneratedKey() {
        stubGeneratedUserId(42);

        int userId = repository.createUser("alice", "hashed", "Student");

        assertThat(userId).isEqualTo(42);
        verify(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }

    @Test
    void createTeacher_executesInsert() {
        repository.createTeacher(10, "Alice", "123", "alice@test.com");

        verify(jdbcTemplate).update("INSERT INTO teacher (user_id, name, contact, email) VALUES (?, ?, ?, ?)",
                10, "Alice", "123", "alice@test.com");
    }

    @Test
    void createStudent_executesInsert() {
        repository.createStudent(20, "Bob", "456", "bob@test.com");

        verify(jdbcTemplate).update("INSERT INTO student (user_id, name, contact, email) VALUES (?, ?, ?, ?)",
                20, "Bob", "456", "bob@test.com");
    }

    @Test
    void updateRequestStatus_executesUpdate() {
        when(jdbcTemplate.update("UPDATE request SET status = ? WHERE request_id = ?", "Approved", 1)).thenReturn(1);

        assertThat(repository.updateRequestStatus(1, "Approved")).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllTeacher_returnsList() {
        Teacher t = new Teacher();
        t.setTeacherId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(t));

        List<Teacher> teachers = repository.getAllTeacher();

        assertThat(teachers).containsExactly(t);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllStudent_returnsList() {
        Student s = new Student();
        s.setStudentId(2);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(s));

        List<Student> students = repository.getAllStudent();

        assertThat(students).containsExactly(s);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteTeacherById_deactivatesATeacherWithExams() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(5)))
                .thenReturn(List.of(50));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE teacher_id = ?"), eq(Integer.class), eq(5)))
                .thenReturn(2);

        assertThat(repository.deleteTeacherById(5)).isTrue();

        verify(jdbcTemplate).update("UPDATE teacher SET status = 'Inactive' WHERE teacher_id = ?", 5);
        verify(jdbcTemplate).update("UPDATE user SET status = 'Inactive' WHERE user_id = ?", 50);
        verify(jdbcTemplate, never()).update("DELETE FROM teacher WHERE teacher_id = ?", 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteTeacherById_hardDeletesATeacherWithoutExams() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(5)))
                .thenReturn(List.of(50));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE teacher_id = ?"), eq(Integer.class), eq(5)))
                .thenReturn(0);
        when(jdbcTemplate.update("DELETE FROM teacher WHERE teacher_id = ?", 5)).thenReturn(1);

        assertThat(repository.deleteTeacherById(5)).isTrue();

        verify(jdbcTemplate).update("DELETE FROM user WHERE user_id = ?", 50);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_deactivatesAStudentWithResults() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of(60));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM result WHERE student_id = ?"), eq(Integer.class), eq(6)))
                .thenReturn(1);

        assertThat(repository.deleteStudentById(6)).isTrue();

        verify(jdbcTemplate).update("UPDATE student SET status = 'Inactive' WHERE student_id = ?", 6);
        verify(jdbcTemplate, never()).update("DELETE FROM student WHERE student_id = ?", 6);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_hardDeletesAStudentWithoutResults() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of(60));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM result WHERE student_id = ?"), eq(Integer.class), eq(6)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE student_id = ?"),
                eq(Integer.class), eq(6))).thenReturn(0);
        when(jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", 6)).thenReturn(1);

        assertThat(repository.deleteStudentById(6)).isTrue();

        verify(jdbcTemplate).update("DELETE FROM user WHERE user_id = ?", 60);
        verify(jdbcTemplate, never()).update(eq("DELETE FROM exam_registration WHERE student_id = ?"), eq(6));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_deactivatesWhenOnlyAnUnsubmittedAttemptExists() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of(60));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM result WHERE student_id = ?"), eq(Integer.class), eq(6)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE student_id = ?"),
                eq(Integer.class), eq(6))).thenReturn(1);

        assertThat(repository.deleteStudentById(6)).isTrue();

        verify(jdbcTemplate).update("UPDATE student SET status = 'Inactive' WHERE student_id = ?", 6);
        verify(jdbcTemplate).update("UPDATE user SET status = 'Inactive' WHERE user_id = ?", 60);
        verify(jdbcTemplate, never()).update("DELETE FROM student WHERE student_id = ?", 6);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_reportsAMissingStudentWithoutTouchingTheUserTable() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM result WHERE student_id = ?"), eq(Integer.class), eq(6)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE student_id = ?"),
                eq(Integer.class), eq(6))).thenReturn(0);
        when(jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", 6)).thenReturn(0);

        assertThat(repository.deleteStudentById(6)).isFalse();

        verify(jdbcTemplate, never()).update(eq("DELETE FROM user WHERE user_id = ?"), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleStudentStatus_writesTheProfileAndTheLogin() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(22)))
                .thenReturn(List.of(70));
        when(jdbcTemplate.update("UPDATE student SET status = ? WHERE student_id = ?", "Inactive", 22)).thenReturn(1);

        assertThat(repository.toggleStudentStatus(22, "Inactive")).isTrue();

        verify(jdbcTemplate).update("UPDATE user SET status = ? WHERE user_id = ?", "Inactive", 70);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleStudentStatus_writesBothRowsBackToActive() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(22)))
                .thenReturn(List.of(70));
        when(jdbcTemplate.update("UPDATE student SET status = ? WHERE student_id = ?", "Active", 22)).thenReturn(1);

        assertThat(repository.toggleStudentStatus(22, "Active")).isTrue();

        verify(jdbcTemplate).update("UPDATE user SET status = ? WHERE user_id = ?", "Active", 70);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleTeacherStatus_writesTheProfileAndTheLogin() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(11)))
                .thenReturn(List.of(40));
        when(jdbcTemplate.update("UPDATE teacher SET status = ? WHERE teacher_id = ?", "Inactive", 11)).thenReturn(1);

        assertThat(repository.toggleTeacherStatus(11, "Inactive")).isTrue();

        verify(jdbcTemplate).update("UPDATE user SET status = ? WHERE user_id = ?", "Inactive", 40);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleTeacherStatus_writesBothRowsBackToActive() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(11)))
                .thenReturn(List.of(40));
        when(jdbcTemplate.update("UPDATE teacher SET status = ? WHERE teacher_id = ?", "Active", 11)).thenReturn(1);

        assertThat(repository.toggleTeacherStatus(11, "Active")).isTrue();

        verify(jdbcTemplate).update("UPDATE user SET status = ? WHERE user_id = ?", "Active", 40);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleStatus_doesNotUpdateTheLoginWhenTheProfileRowIsGone() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(11)))
                .thenReturn(List.of());
        when(jdbcTemplate.update("UPDATE teacher SET status = ? WHERE teacher_id = ?", "Active", 11)).thenReturn(0);

        assertThat(repository.toggleTeacherStatus(11, "Active")).isFalse();

        verify(jdbcTemplate, never()).update(eq("UPDATE user SET status = ? WHERE user_id = ?"), any(), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void toggleStatus_doesNotSilentlySucceedWhenTheLoginUpdateFails() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(22)))
                .thenReturn(List.of(70));
        when(jdbcTemplate.update("UPDATE student SET status = ? WHERE student_id = ?", "Inactive", 22)).thenReturn(1);
        when(jdbcTemplate.update("UPDATE user SET status = ? WHERE user_id = ?", "Inactive", 70))
                .thenThrow(new BadSqlGrammarException(
                        "UPDATE user SET status = ? WHERE user_id = ?",
                        "Unknown column 'status' in 'field list'",
                        new SQLException("Unknown column 'status' in 'field list'")));

        assertThatThrownBy(() -> repository.toggleStudentStatus(22, "Inactive"))
                .isInstanceOf(BadSqlGrammarException.class)
                .hasMessageContaining("Unknown column 'status'");

        verify(jdbcTemplate, never()).update(eq("UPDATE user SET status = ? WHERE user_id = ?"), eq("Active"), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_doesNotSwallowADatabaseFailure() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of(60));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM result WHERE student_id = ?"), eq(Integer.class), eq(6)))
                .thenReturn(1);
        when(jdbcTemplate.update("UPDATE student SET status = 'Inactive' WHERE student_id = ?", 6))
                .thenThrow(new BadSqlGrammarException(
                        "UPDATE student SET status = 'Inactive' WHERE student_id = ?",
                        "Unknown column 'status' in 'field list'",
                        new SQLException("Unknown column 'status' in 'field list'")));

        assertThatThrownBy(() -> repository.deleteStudentById(6))
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllStudent_reportsTheStoredStatus() throws Exception {
        Student student = studentRow("Inactive");

        List<Student> students = mapStudents(student);

        assertThat(students.get(0).getStatus()).isEqualTo("Inactive");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllTeacher_reportsTheStoredStatus() throws Exception {
        Teacher teacher = teacherRow("Active");

        List<Teacher> teachers = mapTeachers(teacher);

        assertThat(teachers.get(0).getStatus()).isEqualTo("Active");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllStudent_fallsBackToActiveForANullStatus() throws Exception {
        Student student = studentRow(null);

        List<Student> students = mapStudents(student);

        assertThat(students.get(0).getStatus()).isEqualTo("Active");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllStudent_doesNotHideAMissingStatusColumn() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("student_id")).thenReturn(1);
        when(resultSet.getInt("user_id")).thenReturn(2);
        when(resultSet.getString("name")).thenReturn("Alice");
        when(resultSet.getString("email")).thenReturn("alice@test.com");
        when(resultSet.getString("contact")).thenReturn("123");
        when(resultSet.getString("profile_image")).thenReturn(null);
        when(resultSet.getString("status")).thenThrow(new SQLException("Unknown column 'status'"));
        RowMapper<Student> mapper = studentRowMapper();
        when(jdbcTemplate.query(eq("SELECT * FROM student ORDER BY student_id DESC"), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Student> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        assertThatThrownBy(() -> repository.getAllStudent()).isInstanceOf(SQLException.class);
        assertThat(mapper).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private List<Student> mapStudents(Student student) throws Exception {
        ResultSet resultSet = mockStudentResultSet(student.getStatus());
        when(jdbcTemplate.query(eq("SELECT * FROM student ORDER BY student_id DESC"), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Student> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });
        return repository.getAllStudent();
    }

    @SuppressWarnings("unchecked")
    private List<Teacher> mapTeachers(Teacher teacher) throws Exception {
        ResultSet resultSet = mockTeacherResultSet(teacher.getStatus());
        when(jdbcTemplate.query(eq("SELECT * FROM teacher ORDER BY teacher_id DESC"), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Teacher> rowMapper = invocation.getArgument(1);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });
        return repository.getAllTeacher();
    }

    private ResultSet mockStudentResultSet(String status) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("student_id")).thenReturn(1);
        when(resultSet.getInt("user_id")).thenReturn(2);
        when(resultSet.getString("name")).thenReturn("Alice");
        when(resultSet.getString("email")).thenReturn("alice@test.com");
        when(resultSet.getString("contact")).thenReturn("123");
        when(resultSet.getString("profile_image")).thenReturn(null);
        when(resultSet.getString("status")).thenReturn(status);
        return resultSet;
    }

    private ResultSet mockTeacherResultSet(String status) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("teacher_id")).thenReturn(1);
        when(resultSet.getInt("user_id")).thenReturn(2);
        when(resultSet.getString("name")).thenReturn("Alice");
        when(resultSet.getString("email")).thenReturn("alice@test.com");
        when(resultSet.getString("contact")).thenReturn("123");
        when(resultSet.getString("profile_image")).thenReturn(null);
        when(resultSet.getString("status")).thenReturn(status);
        return resultSet;
    }

    private Student studentRow(String status) {
        Student student = new Student();
        student.setStatus(status);
        return student;
    }

    private Teacher teacherRow(String status) {
        Teacher teacher = new Teacher();
        teacher.setStatus(status);
        return teacher;
    }

    private RowMapper<Student> studentRowMapper() {
        return (rs, rowNum) -> new Student();
    }

    @Test
    void getAdminStats_returnsGatheredCounts() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(10);

        AdminStatsResponse stats = repository.getAdminStats();

        assertThat(stats.getTotalStudents()).isEqualTo(10);
        assertThat(stats.getTotalTeachers()).isEqualTo(10);
        assertThat(stats.getTotalExams()).isEqualTo(10);
        assertThat(stats.getPendingExams()).isEqualTo(10);
        assertThat(stats.getPendingRequests()).isEqualTo(10);
    }

    @Test
    void getAdminStats_propagatesDatabaseFailure_insteadOfReturningZeroedStats() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM student"), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAdminStats())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
