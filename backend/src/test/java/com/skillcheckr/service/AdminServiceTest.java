package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void addStudentFromRequest_returnsFalse_andSkipsInsert_whenUsernameAlreadyExists() {
        when(adminRepository.getUsernameByRequestId(1)).thenReturn("takenUser");
        when(adminRepository.isUsernameExist("takenUser")).thenReturn(true);

        boolean result = adminService.addStudentFromRequest(1);

        assertThat(result).isFalse();
        verify(adminRepository, never()).addStudentFromRequest(1);
    }

    @Test
    void addStudentFromRequest_returnsTrue_whenUsernameIsFreeAndInsertSucceeds() {
        when(adminRepository.getUsernameByRequestId(1)).thenReturn("newUser");
        when(adminRepository.isUsernameExist("newUser")).thenReturn(false);
        when(adminRepository.addStudentFromRequest(1)).thenReturn(true);

        boolean result = adminService.addStudentFromRequest(1);

        assertThat(result).isTrue();
        verify(adminRepository).addStudentFromRequest(1);
    }

    @Test
    void addStudentFromRequest_returnsFalse_whenInsertFails() {
        when(adminRepository.getUsernameByRequestId(2)).thenReturn("freeUser");
        when(adminRepository.isUsernameExist("freeUser")).thenReturn(false);
        when(adminRepository.addStudentFromRequest(2)).thenReturn(false);

        assertThat(adminService.addStudentFromRequest(2)).isFalse();
    }

    @Test
    void addTeacherFromRequest_returnsFalseWhenUsernameAlreadyExists() {
        when(adminRepository.getUsernameByRequestId(3)).thenReturn("dupTeacher");
        when(adminRepository.isUsernameExist("dupTeacher")).thenReturn(true);

        boolean result = adminService.addTeacherFromRequest(3);

        assertThat(result).isFalse();
        verify(adminRepository, never()).addTeacherFromRequest(3);
    }

    @Test
    void addTeacherFromRequest_returnsTrue_whenUsernameIsFreeAndInsertSucceeds() {
        when(adminRepository.getUsernameByRequestId(3)).thenReturn("newTeacher");
        when(adminRepository.isUsernameExist("newTeacher")).thenReturn(false);
        when(adminRepository.addTeacherFromRequest(3)).thenReturn(true);

        boolean result = adminService.addTeacherFromRequest(3);

        assertThat(result).isTrue();
        verify(adminRepository).addTeacherFromRequest(3);
    }

    @Test
    void getName_and_getRole_areDelegated() {
        when(adminRepository.getUsernameByRequestId(9)).thenReturn("someone");

        assertThat(adminService.getUsernameByRequestId(9)).isEqualTo("someone");
        assertThat(adminService.isUsernameExist("someone")).isFalse();
        verify(adminRepository).getUsernameByRequestId(9);
        verify(adminRepository).isUsernameExist("someone");
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
}