package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.repository.AdminRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void addStudentFromRequest_returnsTrue_whenRepositorySucceeds() {
        when(adminRepository.getUsernameByRequestId(1)).thenReturn(Optional.empty());
        when(adminRepository.addStudentFromRequest(1)).thenReturn(true);

        boolean result = adminService.addStudentFromRequest(1);

        assertThat(result).isTrue();
        verify(adminRepository).addStudentFromRequest(1);
    }

    @Test
    void addStudentFromRequest_returnsFalse_whenRepositoryFails() {
        when(adminRepository.getUsernameByRequestId(2)).thenReturn(Optional.empty());
        when(adminRepository.addStudentFromRequest(2)).thenReturn(false);

        boolean result = adminService.addStudentFromRequest(2);

        assertThat(result).isFalse();
        verify(adminRepository).addStudentFromRequest(2);
    }

    @Test
    void addTeacherFromRequest_returnsTrue_whenRepositorySucceeds() {
        when(adminRepository.getUsernameByRequestId(3)).thenReturn(Optional.empty());
        when(adminRepository.addTeacherFromRequest(3)).thenReturn(true);

        boolean result = adminService.addTeacherFromRequest(3);

        assertThat(result).isTrue();
        verify(adminRepository).addTeacherFromRequest(3);
    }

    @Test
    void addTeacherFromRequest_returnsFalse_whenRepositoryFails() {
        when(adminRepository.getUsernameByRequestId(4)).thenReturn(Optional.empty());
        when(adminRepository.addTeacherFromRequest(4)).thenReturn(false);

        boolean result = adminService.addTeacherFromRequest(4);

        assertThat(result).isFalse();
        verify(adminRepository).addTeacherFromRequest(4);
    }

    @Test
    void isUsernameExist_delegatesToRepository() {
        when(adminRepository.isUsernameExist("someone")).thenReturn(true);

        assertThat(adminService.isUsernameExist("someone")).isTrue();
        verify(adminRepository).isUsernameExist("someone");
    }

    @Test
    void getUsernameByRequestId_delegatesToRepository() {
        when(adminRepository.getUsernameByRequestId(9)).thenReturn(Optional.of("someone"));

        assertThat(adminService.getUsernameByRequestId(9)).contains("someone");
        verify(adminRepository).getUsernameByRequestId(9);
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
    void getAdminStats_delegatesToRepository() {
        AdminStatsResponse stats = AdminStatsResponse.builder().totalStudents(5).build();
        when(adminRepository.getAdminStats()).thenReturn(stats);

        assertThat(adminService.getAdminStats()).isEqualTo(stats);
        verify(adminRepository).getAdminStats();
    }
}