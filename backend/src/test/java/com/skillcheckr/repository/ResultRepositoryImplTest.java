package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.model.ExamSubmissionSummaryDTO;
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

        Optional<ExamResultDTO> result = repository.getResultByExamAndStudent(50, 99);

        assertThat(result).containsSame(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getResultByExamAndStudent_returnsEmpty_whenNotPresent() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(50), eq(99))).thenReturn(List.of());

        Optional<ExamResultDTO> result = repository.getResultByExamAndStudent(50, 99);

        assertThat(result).isEmpty();
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
    @SuppressWarnings("unchecked")
    void findSubmissionSummariesByExamIdPreservesZeroDefaultsForUnsubmittedAttempt() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(25))).thenAnswer(invocation -> {
            RowMapper<ExamSubmissionSummaryDTO> rowMapper = invocation.getArgument(1);
            ResultSet row = mock(ResultSet.class);
            when(row.getInt("attempt_id")).thenReturn(7);
            when(row.getInt("student_id")).thenReturn(9);
            when(row.getInt("marks_obtained")).thenReturn(0);
            when(row.getInt("total_marks")).thenReturn(0);
            when(row.getInt("passing_marks")).thenReturn(0);
            when(row.getDouble("percentage")).thenReturn(0.0);
            when(row.getObject("started_at", LocalDateTime.class)).thenReturn(null);
            when(row.getObject("submitted_at", LocalDateTime.class)).thenReturn(null);

            return List.of(rowMapper.mapRow(row, 0));
        });

        ExamSubmissionSummaryDTO summary = repository.findSubmissionSummariesByExamId(25).get(0);

        assertThat(summary.getMarksObtained()).isZero();
        assertThat(summary.getTotalMarks()).isZero();
        assertThat(summary.getPassingMarks()).isZero();
        assertThat(summary.getPercentage()).isZero();
    }

    @Test
    void insertSubmissionResult_assignsGeneratedResultId() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("result_id", 42L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of("Alice"));

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
    @SuppressWarnings("unchecked")
    void insertSubmissionResult_resolvesTheStudentNameForTheImmediateResponse() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("result_id", 42L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of("Alice"));

        ExamResultDTO saved = repository.insertSubmissionResult(ExamResultDTO.builder()
                .examId(1)
                .studentId(7)
                .marksObtained(50)
                .totalMarks(100)
                .passingMarks(40)
                .percentage(50.0)
                .status("Pass")
                .submittedAt("2026-09-24 10:00:00")
                .attemptId(1)
                .build());

        assertThat(saved.getStudentName()).isEqualTo("Alice");
    }

    @Test
    @SuppressWarnings("unchecked")
    void insertSubmissionResult_leavesTheNameNullWhenTheStudentRowIsGone() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("result_id", 42L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of());

        ExamResultDTO saved = repository.insertSubmissionResult(ExamResultDTO.builder()
                .examId(1)
                .studentId(7)
                .marksObtained(50)
                .totalMarks(100)
                .passingMarks(40)
                .percentage(50.0)
                .status("Pass")
                .submittedAt("2026-09-24 10:00:00")
                .attemptId(1)
                .build());

        assertThat(saved.getStudentName()).isNull();
    }

    @Test
    void insertSubmissionResult_rejectsAMissingSubmittedAt() {
        ExamResultDTO result = ExamResultDTO.builder()
                .examId(1)
                .studentId(7)
                .marksObtained(50)
                .totalMarks(100)
                .passingMarks(40)
                .percentage(50.0)
                .status("Pass")
                .attemptId(1)
                .build();

        assertThatThrownBy(() -> repository.insertSubmissionResult(result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("submitted_at is required");
    }

    @Test
    void insertSubmissionResult_rejectsAnUnreadableSubmittedAt() {
        ExamResultDTO result = ExamResultDTO.builder()
                .examId(1)
                .studentId(7)
                .marksObtained(50)
                .totalMarks(100)
                .passingMarks(40)
                .percentage(50.0)
                .status("Pass")
                .submittedAt("2026-09-24T10:00:00")
                .attemptId(1)
                .build();

        assertThatThrownBy(() -> repository.insertSubmissionResult(result))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd HH:mm:ss");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByAttemptId_returnsFromDb() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).attemptId(3).build();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3))).thenReturn(List.of(dto));

        assertThat(repository.findByAttemptId(3)).containsSame(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByAttemptId_returnsEmpty_whenNotPresent() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3))).thenReturn(List.of());

        assertThat(repository.findByAttemptId(3)).isEmpty();
    }

    @Test
    void updateResultAfterEvaluation_reportsSuccess_whenARowChanged() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        boolean updated = repository.updateResultAfterEvaluation(3, 9, 10, 4, 90.0, "Pass");

        assertThat(updated).isTrue();
    }

    @Test
    void updateResultAfterEvaluation_reportsFailure_whenNoRowChanged() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

        assertThat(repository.updateResultAfterEvaluation(3, 9, 10, 4, 90.0, "Pass")).isFalse();
    }
}
