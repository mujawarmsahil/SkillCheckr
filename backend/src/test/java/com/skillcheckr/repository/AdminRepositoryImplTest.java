package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

@ExtendWith(MockitoExtension.class)
class AdminRepositoryImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement psSelect;

    @Mock
    private PreparedStatement psCheck;

    @Mock
    private PreparedStatement psUser;

    @Mock
    private PreparedStatement psRole;

    @Mock
    private PreparedStatement psUpdate;

    @Mock
    private ResultSet rsSelect;

    @Mock
    private ResultSet rsCheck;

    @Mock
    private ResultSet rsGenKeys;

    @Mock
    private ResultSet rsRole;

    @InjectMocks
    private AdminRepositoryImpl repository;

    @Test
    void addStudentFromRequest_returnsTrue_whenNewUserAndStudentCreated() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM request WHERE request_id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rsSelect);
        when(rsSelect.next()).thenReturn(true);
        when(rsSelect.getString("username")).thenReturn("alice");
        when(rsSelect.getString("password")).thenReturn("plainPass");
        when(rsSelect.getString("name")).thenReturn("Alice");
        when(rsSelect.getString("contact")).thenReturn("123");
        when(rsSelect.getString("email")).thenReturn("alice@test.com");

        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashedPass");

        when(connection.prepareStatement("SELECT user_id FROM user WHERE username = ?")).thenReturn(psCheck);
        when(psCheck.executeQuery()).thenReturn(rsCheck);
        when(rsCheck.next()).thenReturn(false); // New user

        when(connection.prepareStatement(eq("INSERT INTO user (username, password, user_role) VALUES (?, ?, ?)"), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(psUser);
        when(psUser.getGeneratedKeys()).thenReturn(rsGenKeys);
        when(rsGenKeys.next()).thenReturn(true);
        when(rsGenKeys.getInt(1)).thenReturn(10); // user_id = 10

        when(connection.prepareStatement("SELECT student_id FROM student WHERE user_id = ? OR email = ?")).thenReturn(psRole);
        when(psRole.executeQuery()).thenReturn(rsRole);
        when(rsRole.next()).thenReturn(false); // Not existing yet

        PreparedStatement psInsertStudent = org.mockito.Mockito.mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO student(user_id, name, contact, email) VALUES (?, ?, ?, ?)")).thenReturn(psInsertStudent);

        when(connection.prepareStatement("UPDATE request SET status = 'Approved' WHERE request_id = ?")).thenReturn(psUpdate);

        boolean success = repository.addStudentFromRequest(1);

        assertThat(success).isTrue();
        verify(psUpdate).executeUpdate();
    }

    @Test
    void addStudentFromRequest_returnsFalse_whenUsernameTakenBeforeApproval() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM request WHERE request_id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rsSelect);
        when(rsSelect.next()).thenReturn(true);
        when(rsSelect.getString("username")).thenReturn("alice");
        when(rsSelect.getString("password")).thenReturn("plainPass");
        when(rsSelect.getString("name")).thenReturn("Alice");
        when(rsSelect.getString("contact")).thenReturn("123");
        when(rsSelect.getString("email")).thenReturn("alice@test.com");

        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashedPass");

        when(connection.prepareStatement("SELECT user_id FROM user WHERE username = ?")).thenReturn(psCheck);
        when(psCheck.executeQuery()).thenReturn(rsCheck);
        when(rsCheck.next()).thenReturn(false);

        when(connection.prepareStatement(eq("INSERT INTO user (username, password, user_role) VALUES (?, ?, ?)"),
                eq(PreparedStatement.RETURN_GENERATED_KEYS))).thenReturn(psUser);
        when(psUser.executeUpdate())
                .thenThrow(new DuplicateKeyException("Duplicate entry 'alice' for key 'user.username'"));

        boolean success = repository.addStudentFromRequest(1);

        assertThat(success).isFalse();
    }

    @Test
    void addStudentFromRequest_returnsFalse_whenRequestNotFound() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM request WHERE request_id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rsSelect);
        when(rsSelect.next()).thenReturn(false);

        boolean success = repository.addStudentFromRequest(99);

        assertThat(success).isFalse();
    }

    @Test
    void addTeacherFromRequest_returnsTrue_whenExistingUserAndNewTeacherCreated() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT * FROM request WHERE request_id = ?")).thenReturn(psSelect);
        when(psSelect.executeQuery()).thenReturn(rsSelect);
        when(rsSelect.next()).thenReturn(true);
        when(rsSelect.getString("username")).thenReturn("bob");
        when(rsSelect.getString("password")).thenReturn("$2a$10$alreadyHashed");
        when(rsSelect.getString("name")).thenReturn("Bob");
        when(rsSelect.getString("contact")).thenReturn("456");
        when(rsSelect.getString("email")).thenReturn("bob@test.com");

        when(connection.prepareStatement("SELECT user_id FROM user WHERE username = ?")).thenReturn(psCheck);
        when(psCheck.executeQuery()).thenReturn(rsCheck);
        when(rsCheck.next()).thenReturn(true);
        when(rsCheck.getInt("user_id")).thenReturn(20);

        when(connection.prepareStatement("SELECT teacher_id FROM teacher WHERE user_id = ? OR email = ?")).thenReturn(psRole);
        when(psRole.executeQuery()).thenReturn(rsRole);
        when(rsRole.next()).thenReturn(false);

        PreparedStatement psInsertTeacher = org.mockito.Mockito.mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO teacher(user_id, name, contact, email) VALUES (?, ?, ?, ?)")).thenReturn(psInsertTeacher);

        when(connection.prepareStatement("UPDATE request SET status = 'Approved' WHERE request_id = ?")).thenReturn(psUpdate);

        boolean success = repository.addTeacherFromRequest(2);

        assertThat(success).isTrue();
        verify(psUpdate).executeUpdate();
    }

    @Test
    void isUsernameExist_returnsTrue_whenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("taken"))).thenReturn(1);

        assertThat(repository.isUsernameExist("taken")).isTrue();
    }

    @Test
    void isUsernameExist_returnsFalse_whenCountZeroOrException() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("free"))).thenReturn(0);

        assertThat(repository.isUsernameExist("free")).isFalse();
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
    void deleteTeacherById_deletesTeacherAndUser() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM teacher WHERE teacher_id = ?"), any(RowMapper.class), eq(5)))
                .thenReturn(List.of(50));
        when(jdbcTemplate.update("DELETE FROM teacher WHERE teacher_id = ?", 5)).thenReturn(1);

        boolean result = repository.deleteTeacherById(5);

        assertThat(result).isTrue();
        verify(jdbcTemplate).update("DELETE FROM user WHERE user_id = ?", 50);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteStudentById_deletesStudentAndUser() {
        when(jdbcTemplate.query(eq("SELECT user_id FROM student WHERE student_id = ?"), any(RowMapper.class), eq(6)))
                .thenReturn(List.of(60));
        when(jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", 6)).thenReturn(1);

        boolean result = repository.deleteStudentById(6);

        assertThat(result).isTrue();
        verify(jdbcTemplate).update("DELETE FROM user WHERE user_id = ?", 60);
    }

    @Test
    void getUsernameByRequestId_returnsUsername() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(10))).thenReturn("john_doe");

        Optional<String> username = repository.getUsernameByRequestId(10);

        assertThat(username).contains("john_doe");
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
}
