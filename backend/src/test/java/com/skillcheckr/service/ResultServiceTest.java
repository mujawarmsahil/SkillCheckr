package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.repository.ResultRepository;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @InjectMocks
    private ResultServiceImpl resultService;

    @Test
    void submitExam_delegatesToRepository() {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setExamId(1);
        submission.setStudentId(2);

        ExamResultDTO expected = ExamResultDTO.builder().resultId(10).build();
        when(resultRepository.submitExam(submission)).thenReturn(expected);

        ExamResultDTO actual = resultService.submitExam(submission);

        assertThat(actual).isSameAs(expected);
        verify(resultRepository).submitExam(submission);
    }

    @Test
    void getResultsByStudentId_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getResultsByStudentId(2)).thenReturn(List.of(dto));

        List<ExamResultDTO> actual = resultService.getResultsByStudentId(2);

        assertThat(actual).hasSize(1);
        verify(resultRepository).getResultsByStudentId(2);
    }

    @Test
    void getResultByExamAndStudent_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getResultByExamAndStudent(5, 2)).thenReturn(dto);

        ExamResultDTO actual = resultService.getResultByExamAndStudent(5, 2);

        assertThat(actual).isSameAs(dto);
        verify(resultRepository).getResultByExamAndStudent(5, 2);
    }

    @Test
    void getAllResults_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getAllResults()).thenReturn(List.of(dto));

        List<ExamResultDTO> actual = resultService.getAllResults();

        assertThat(actual).hasSize(1);
        verify(resultRepository).getAllResults();
    }
}
