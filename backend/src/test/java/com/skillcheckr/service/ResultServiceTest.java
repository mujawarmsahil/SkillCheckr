package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.repository.ResultRepository;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private QuestionBreakdownService questionBreakdownService;

    @InjectMocks
    private ResultServiceImpl resultService;

    private static ExamResultDTO result(int resultId, int examId, int attemptId, int studentId) {
        return ExamResultDTO.builder()
                .resultId(resultId)
                .examId(examId)
                .attemptId(attemptId)
                .studentId(studentId)
                .build();
    }

    @Test
    void getResultsByStudentId_attachesTheServerDerivedBreakdown() {
        ExamResultDTO stored = result(1, 5, 12, 2);
        when(resultRepository.getResultsByStudentId(2)).thenReturn(List.of(stored));
        when(questionBreakdownService.build(5, 12)).thenReturn(List.of(Map.of("question_id", 3)));

        List<ExamResultDTO> actual = resultService.getResultsByStudentId(2);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getQuestionBreakdown()).hasSize(1);
        verify(questionBreakdownService).build(5, 12);
    }

    @Test
    void getResultsByStudentId_returnsEmptyList_whenTheRepositoryReturnsNull() {
        when(resultRepository.getResultsByStudentId(2)).thenReturn(null);

        assertThat(resultService.getResultsByStudentId(2)).isEmpty();
    }

    @Test
    void getResultsByStudentId_skipsTheBreakdownForAResultWithoutAnAttempt() {
        when(resultRepository.getResultsByStudentId(2)).thenReturn(List.of(result(1, 5, 0, 2)));

        List<ExamResultDTO> actual = resultService.getResultsByStudentId(2);

        assertThat(actual.get(0).getQuestionBreakdown()).isNull();
        verify(questionBreakdownService, never()).build(anyInt(), anyInt());
    }

    @Test
    void getResultByExamAndStudent_attachesTheBreakdownToTheFoundResult() {
        ExamResultDTO stored = result(1, 5, 12, 2);
        when(resultRepository.getResultByExamAndStudent(5, 2)).thenReturn(Optional.of(stored));
        when(questionBreakdownService.build(5, 12)).thenReturn(List.of());

        assertThat(resultService.getResultByExamAndStudent(5, 2)).isPresent();
        verify(questionBreakdownService).build(5, 12);
    }

    @Test
    void getResultByExamAndStudent_returnsEmpty_whenNoResultExists() {
        when(resultRepository.getResultByExamAndStudent(5, 2)).thenReturn(Optional.empty());

        assertThat(resultService.getResultByExamAndStudent(5, 2)).isEmpty();
        verify(questionBreakdownService, never()).build(anyInt(), anyInt());
    }

    @Test
    void getAllResults_attachesTheBreakdownToEveryResult() {
        when(resultRepository.getAllResults()).thenReturn(List.of(result(1, 5, 12, 2), result(2, 6, 13, 3)));
        when(questionBreakdownService.build(anyInt(), anyInt())).thenReturn(List.of(Map.of("marks", 1)));

        assertThat(resultService.getAllResults()).hasSize(2);
        verify(questionBreakdownService).build(5, 12);
        verify(questionBreakdownService).build(6, 13);
    }
}
