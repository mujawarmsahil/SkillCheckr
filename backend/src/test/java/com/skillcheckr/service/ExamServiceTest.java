package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.ExamRepository;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @InjectMocks
    private ExamServiceImpl examService;

    @Test
    void saveExam_delegatesToRepository() {
        Exam exam = new Exam();
        exam.setExamName("Maths");
        Subject subject = new Subject(1, "Mathematics", "MATH101");
        when(examRepository.saveExam(exam)).thenReturn(subject);

        Subject result = examService.saveExam(exam);

        assertThat(result).isEqualTo(subject);
        verify(examRepository).saveExam(exam);
    }

    @Test
    void viewAllExams_delegatesToRepository() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(examRepository.viewAllExams()).thenReturn(List.of(exam));

        assertThat(examService.viewAllExams()).hasSize(1);
        verify(examRepository).viewAllExams();
    }

    @Test
    void deleteExamById_delegatesToRepository() {
        when(examRepository.deleteExamById(9)).thenReturn(true);

        assertThat(examService.deleteExamById(9)).isTrue();
        verify(examRepository).deleteExamById(9);
    }

    @Test
    void acceptExam_delegatesToRepository() {
        when(examRepository.acceptExam(3)).thenReturn(false);

        assertThat(examService.acceptExam(3)).isFalse();
        verify(examRepository).acceptExam(3);
    }

    @Test
    void viewAllUpcomingExam_delegatesToRepository() {
        Exam upcoming = new Exam();
        upcoming.setStatus("Upcoming");
        upcoming.setStartTime(LocalTime.of(9, 0));
        when(examRepository.viewAllUpcomingExam()).thenReturn(List.of(upcoming));

        assertThat(examService.viewAllUpcomingExam()).hasSize(1);
        verify(examRepository).viewAllUpcomingExam();
    }

    @Test
    void viewAllCompletedExam_delegatesToRepository() {
        Exam completed = new Exam();
        completed.setStatus("Completed");
        when(examRepository.viewAllCompletedExam()).thenReturn(List.of(completed));

        assertThat(examService.viewAllCompletedExam()).hasSize(1);
        verify(examRepository).viewAllCompletedExam();
    }

    @Test
    void getExamById_delegatesToRepository() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(examRepository.getExamById(1)).thenReturn(exam);

        assertThat(examService.getExamById(1)).isSameAs(exam);
        verify(examRepository).getExamById(1);
    }

    @Test
    void getExamsByTeacherId_delegatesToRepository() {
        Exam exam = new Exam();
        when(examRepository.getExamsByTeacherId(10)).thenReturn(List.of(exam));

        assertThat(examService.getExamsByTeacherId(10)).hasSize(1);
        verify(examRepository).getExamsByTeacherId(10);
    }

    @Test
    void registerStudentForExam_delegatesToRepository() {
        when(examRepository.registerStudentForExam(1, 2)).thenReturn(true);

        assertThat(examService.registerStudentForExam(1, 2)).isTrue();
        verify(examRepository).registerStudentForExam(1, 2);
    }

    @Test
    void isStudentRegisteredForExam_delegatesToRepository() {
        when(examRepository.isStudentRegisteredForExam(1, 2)).thenReturn(true);

        assertThat(examService.isStudentRegisteredForExam(1, 2)).isTrue();
        verify(examRepository).isStudentRegisteredForExam(1, 2);
    }

    @Test
    void getRegisteredExamIdsForStudent_delegatesToRepository() {
        when(examRepository.getRegisteredExamIdsForStudent(1)).thenReturn(List.of(2, 3));

        assertThat(examService.getRegisteredExamIdsForStudent(1)).containsExactly(2, 3);
        verify(examRepository).getRegisteredExamIdsForStudent(1);
    }

    @Test
    void getRegistrationsByStudentId_delegatesToRepository() {
        ExamRegistration reg = new ExamRegistration();
        when(examRepository.getRegistrationsByStudentId(1)).thenReturn(List.of(reg));

        assertThat(examService.getRegistrationsByStudentId(1)).containsExactly(reg);
        verify(examRepository).getRegistrationsByStudentId(1);
    }

    @Test
    void getRegisteredStudentsByExamId_delegatesToRepository() {
        Student student = new Student();
        when(examRepository.getRegisteredStudentsByExamId(2)).thenReturn(List.of(student));

        assertThat(examService.getRegisteredStudentsByExamId(2)).containsExactly(student);
        verify(examRepository).getRegisteredStudentsByExamId(2);
    }

    @Test
    void getRegistrationCountByExamId_delegatesToRepository() {
        when(examRepository.getRegistrationCountByExamId(2)).thenReturn(7);

        assertThat(examService.getRegistrationCountByExamId(2)).isEqualTo(7);
        verify(examRepository).getRegistrationCountByExamId(2);
    }

    @Test
    void unregisterStudentFromExam_delegatesToRepository() {
        when(examRepository.unregisterStudentFromExam(1, 2)).thenReturn(true);

        assertThat(examService.unregisterStudentFromExam(1, 2)).isTrue();
        verify(examRepository).unregisterStudentFromExam(1, 2);
    }
}
