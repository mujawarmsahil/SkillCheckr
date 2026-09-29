package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.repository.AdminRepository;
import com.skillcheckr.repository.ExamRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private RegistrationRequest pendingRequest(String role) {
        RegistrationRequest request = new RegistrationRequest();
        request.setRequestId(1);
        request.setName("Alice");
        request.setEmail("alice@test.com");
        request.setUsername("alice");
        request.setPassword("plainPass");
        request.setContact("123");
        request.setRequestedRole(role);
        request.setStatus("Pending");
        return request;
    }

    @Test
    void addStudentFromRequest_createsTheAccountAndProfile() {
        RegistrationRequest req = pendingRequest("Student");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));
        when(adminRepository.existsUserByUsername("alice")).thenReturn(false);
        when(adminRepository.existsStudentByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashedPass");
        when(adminRepository.createUser("alice", "$2a$10$hashedPass", "Student")).thenReturn(10);

        assertThat(adminService.addStudentFromRequest(1)).isTrue();

        verify(adminRepository).createStudent(10, "Alice", "123", "alice@test.com");
        verify(adminRepository).updateRequestStatus(1, "Approved");
    }

    @Test
    void addTeacherFromRequest_createsTheAccountAndProfile() {
        RegistrationRequest req = pendingRequest("Teacher");
        req.setRequestId(2);
        req.setUsername("bob");
        when(adminRepository.findRequestById(2)).thenReturn(Optional.of(req));
        when(adminRepository.existsUserByUsername("bob")).thenReturn(false);
        when(adminRepository.existsTeacherByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashedPass");
        when(adminRepository.createUser("bob", "$2a$10$hashedPass", "Teacher")).thenReturn(20);

        assertThat(adminService.addTeacherFromRequest(2)).isTrue();

        verify(adminRepository).createTeacher(20, "Alice", "123", "alice@test.com");
        verify(adminRepository).updateRequestStatus(2, "Approved");
    }

    @Test
    void addUserFromRequest_refusesWhenTheUsernameIsAlreadyTaken() {
        RegistrationRequest req = pendingRequest("Student");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));
        when(adminRepository.existsUserByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> adminService.addStudentFromRequest(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An account with the username 'alice' already exists");
        verify(adminRepository, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void addUserFromRequest_refusesWhenTheEmailIsAlreadyUsed() {
        RegistrationRequest req = pendingRequest("Student");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));
        when(adminRepository.existsUserByUsername("alice")).thenReturn(false);
        when(adminRepository.existsStudentByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.addStudentFromRequest(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An account with the email 'alice@test.com' already exists");
        verify(adminRepository, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void addUserFromRequest_refusesWhenTheRequestedRoleDoesNotMatch() {
        RegistrationRequest req = pendingRequest("Teacher");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> adminService.addStudentFromRequest(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be approved as Student");
        verify(adminRepository, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void addUserFromRequest_refusesAnAlreadyApprovedRequest() {
        RegistrationRequest req = pendingRequest("Student");
        req.setStatus("Approved");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> adminService.addStudentFromRequest(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This request has already been approved");
    }

    @Test
    void addUserFromRequest_reportsAMissingRequest() {
        when(adminRepository.findRequestById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.addStudentFromRequest(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Registration request not found");
    }

    @Test
    void addUserFromRequest_keepsAnAlreadyHashedPassword() {
        RegistrationRequest req = pendingRequest("Student");
        req.setPassword("$2a$10$alreadyHashed");
        when(adminRepository.findRequestById(1)).thenReturn(Optional.of(req));
        when(adminRepository.existsUserByUsername("alice")).thenReturn(false);
        when(adminRepository.existsStudentByEmail("alice@test.com")).thenReturn(false);
        when(adminRepository.createUser("alice", "$2a$10$alreadyHashed", "Student")).thenReturn(11);

        adminService.addStudentFromRequest(1);

        verify(passwordEncoder, never()).encode(anyString());
        verify(adminRepository).createUser("alice", "$2a$10$alreadyHashed", "Student");
    }

    @Test
    void getAllTeacher_returnsTeachersFromRepository() {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(1);
        teacher.setTeacherName("Alice");
        when(adminRepository.getAllTeacher()).thenReturn(List.of(teacher));

        List<Teacher> teachers = adminService.getAllTeacher();

        assertThat(teachers).hasSize(1);
        assertThat(teachers.get(0).getTeacherName()).isEqualTo("Alice");
    }

    @Test
    void getAllStudent_returnsStudentsFromRepository() {
        Student student = new Student();
        student.setStudentId(2);
        student.setStudentName("Bob");
        when(adminRepository.getAllStudent()).thenReturn(List.of(student));

        List<Student> students = adminService.getAllStudent();

        assertThat(students).hasSize(1);
        assertThat(students.get(0).getStudentName()).isEqualTo("Bob");
    }

    @Test
    void deleteTeacherById_delegatesToRepository() {
        when(adminRepository.deleteTeacherById(11)).thenReturn(true);

        assertThat(adminService.deleteTeacherById(11)).isTrue();
        verify(adminRepository).deleteTeacherById(11);
    }

    @Test
    void deleteStudentById_delegatesToRepository() {
        when(adminRepository.deleteStudentById(22)).thenReturn(false);

        assertThat(adminService.deleteStudentById(22)).isFalse();
        verify(adminRepository).deleteStudentById(22);
    }

    @Test
    void toggleTeacherStatus_delegatesToRepository() {
        when(adminRepository.toggleTeacherStatus(11, "Active")).thenReturn(true);

        assertThat(adminService.toggleTeacherStatus(11, "Active")).isTrue();
        verify(adminRepository).toggleTeacherStatus(11, "Active");
    }

    @Test
    void toggleStudentStatus_delegatesToRepository() {
        when(adminRepository.toggleStudentStatus(22, "Inactive")).thenReturn(true);

        assertThat(adminService.toggleStudentStatus(22, "Inactive")).isTrue();
        verify(adminRepository).toggleStudentStatus(22, "Inactive");
    }

    @Test
    void getAdminStats_syncsExamStatusesAndDelegatesToRepository() {
        AdminStatsResponse stats = AdminStatsResponse.builder().totalStudents(5).build();
        when(adminRepository.getAdminStats()).thenReturn(stats);

        assertThat(adminService.getAdminStats()).isEqualTo(stats);
        verify(examRepository).syncExamStatuses();
        verify(adminRepository).getAdminStats();
    }
}