package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.model.Subject;

@ExtendWith(MockitoExtension.class)
class SubjectRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SubjectRepositoryImpl repository;

    @Test
    void getAllSubjects_propagatesDatabaseFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAllSubjects())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getSubjectById_returnsEmpty_whenTheSubjectDoesNotExist() {
        when(jdbcTemplate.queryForObject(eq("SELECT * FROM subject WHERE subject_id = ?"), any(RowMapper.class), eq(9)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.getSubjectById(9)).isEmpty();
    }

    @Test
    void getSubjectById_propagatesDatabaseFailureInsteadOfReturningEmpty() {
        when(jdbcTemplate.queryForObject(eq("SELECT * FROM subject WHERE subject_id = ?"), any(RowMapper.class), eq(9)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getSubjectById(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void addSubject_rejectsABlankNameWithoutTouchingTheDatabase() {
        Subject subject = new Subject();
        subject.setSubjectName("  ");

        assertThat(repository.addSubject(subject)).isNull();
    }

    @Test
    void addSubject_propagatesDatabaseFailureInsteadOfReturningNull() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM subject WHERE subject_code = ?"),
                eq(Integer.class), anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.addSubject(subject))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void updateSubject_returnsFalse_whenTheSubjectDoesNotExist() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");

        when(jdbcTemplate.update(anyString(), eq("Maths"), eq("MATH"), eq(9))).thenReturn(0);

        assertThat(repository.updateSubject(9, subject)).isFalse();
    }

    @Test
    void updateSubject_propagatesDatabaseFailureInsteadOfReturningFalse() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");

        when(jdbcTemplate.update(anyString(), eq("Maths"), eq("MATH"), eq(9)))
                .thenThrow(new DataAccessResourceFailureException("read only"));

        assertThatThrownBy(() -> repository.updateSubject(9, subject))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getQuestionCountBySubjectId_propagatesDatabaseFailureInsteadOfReturningZero() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM question WHERE subject_id = ?"),
                eq(Integer.class), eq(9)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionCountBySubjectId(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getExamCountBySubjectId_propagatesDatabaseFailureInsteadOfReturningZero() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE subject_id = ?"),
                eq(Integer.class), eq(9)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getExamCountBySubjectId(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void deleteSubjectById_refusesWhenAnExamHasRecordedAttempts() {
        when(jdbcTemplate.query(eq("SELECT exam_id FROM exam WHERE subject_id = ?"),
                any(org.springframework.jdbc.core.RowMapper.class), eq(9)))
                .thenReturn(List.of(4));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE exam_id = ?"),
                eq(Integer.class), eq(4)))
                .thenReturn(2);

        assertThatThrownBy(() -> repository.deleteSubjectById(9))
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    void addSubject_insertsAndReturnsTheGeneratedId() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM subject WHERE subject_code = ?"),
                eq(Integer.class), eq("MATH"))).thenReturn(0);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            Map<String, Object> key = new HashMap<>();
            key.put("key", 12);
            keyHolder.getKeyList().add(key);
            return 1;
        });

        Optional<Subject> created = Optional.ofNullable(repository.addSubject(subject));

        assertThat(created).isPresent();
        assertThat(created.get().getSubjectId()).isEqualTo(12);
    }
}
