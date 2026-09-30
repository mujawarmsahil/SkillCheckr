package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ExamQuestionRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExamQuestionRepositoryImpl repository;

    @Test
    void attachQuestion_reportsTrueOnlyWhenARowIsInserted() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3), eq(10))).thenReturn(0);
        when(jdbcTemplate.update(anyString(), eq(3), eq(10), eq(1))).thenReturn(1);

        assertThat(repository.attachQuestion(3, 10, 1)).isTrue();
    }

    @Test
    void attachQuestion_reportsFalseForAnAlreadyAttachedQuestion() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3), eq(10))).thenReturn(1);

        assertThat(repository.attachQuestion(3, 10, 1)).isFalse();
    }

    @Test
    void attachQuestion_skipsTheInsertWhenTheQuestionIsAlreadyAttached() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3), eq(10))).thenReturn(1);

        repository.attachQuestion(3, 10, 1);

        // The existence check keeps the insert away from the unique constraint.
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void getNextQuestionOrder_continuesAfterTheHighestExistingOrder() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3))).thenReturn(4);

        assertThat(repository.getNextQuestionOrder(3)).isEqualTo(5);
    }
}
