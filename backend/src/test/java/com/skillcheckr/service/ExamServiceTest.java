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
}