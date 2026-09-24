package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.model.ExamResultDTO;

@ExtendWith(MockitoExtension.class)
class ResultRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ResultRepositoryImpl repository;

    @Test
    @SuppressWarnings("unchecked")
    void getResultsByStudentId_returnsFromDb() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).studentId(99).build();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99))).thenReturn(List.of(dto));

        List<ExamResultDTO> results = repository.getResultsByStudentId(99);

        assertThat(results).containsExactly(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getResultByExamAndStudent_returnsFromDb() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).examId(50).studentId(99).build();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(50), eq(99))).thenReturn(List.of(dto));

        ExamResultDTO result = repository.getResultByExamAndStudent(50, 99);

        assertThat(result).isSameAs(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getResultByExamAndStudent_returnsNull_whenNotPresent() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(50), eq(99))).thenReturn(List.of());

        ExamResultDTO result = repository.getResultByExamAndStudent(50, 99);

        assertThat(result).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllResults_returnsFromDb() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(dto));

        List<ExamResultDTO> results = repository.getAllResults();

        assertThat(results).isNotEmpty();
    }

    @Test
    void insertSubmissionResult_assignsGeneratedResultId() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("result_id", 42L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        ExamResultDTO result = ExamResultDTO.builder()
                .examId(1)
                .studentId(7)
                .marksObtained(50)
                .totalMarks(100)
                .passingMarks(40)
                .percentage(50.0)
                .status("Pass")
                .submittedAt("2026-09-24 10:00:00")
                .attemptId(1)
                .build();

        ExamResultDTO saved = repository.insertSubmissionResult(result);

        assertThat(saved).isSameAs(result);
        assertThat(saved.getResultId()).isEqualTo(42);
    }

    @Test
    void createSubmittedAttempt_returnsGeneratedKey() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("attempt_id", 77L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        int attemptId = repository.createSubmittedAttempt(1, 7);

        assertThat(attemptId).isEqualTo(77);
    }
}