package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Question;

@ExtendWith(MockitoExtension.class)
class AttemptAnswerRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AttemptAnswerRepositoryImpl repository;

    @Test
    void saveAndReturnUsesGeneratedKeyUpsertAndExplicitSqlNullTypes() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            creator.createPreparedStatement(connection);
            return 1;
        }).when(jdbcTemplate).update(org.mockito.ArgumentMatchers.any(PreparedStatementCreator.class));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5), eq(8))).thenReturn(42);
        AttemptAnswer answer = AttemptAnswer.builder()
                .attempt(ExamAttempt.builder().attemptId(5).build())
                .question(Question.builder().questionId(8).build())
                .build();

        AttemptAnswer saved = repository.saveAndReturn(answer);

        assertThat(saved.getAttemptAnswerId()).isEqualTo(42);
        verify(statement).setNull(3, Types.INTEGER);
        verify(statement).setNull(4, Types.VARCHAR);
        verify(statement).setNull(5, Types.INTEGER);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("attempt_answer_id = LAST_INSERT_ID(attempt_answer_id)");
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq(5), eq(8));
    }

    @Test
    void findByAttemptIdsReturnsEmptyWithoutQueryForEmptyIds() {
        assertThat(repository.findByAttemptIds(List.of())).isEmpty();
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByAttemptIdsUsesOneOrderedQueryForAllAttempts() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        assertThat(repository.findByAttemptIds(List.of(9, 3))).isEmpty();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), arguments.capture());
        assertThat(arguments.getValue()).containsExactly(9, 3);
        assertThat(sql.getValue()).contains("aa.attempt_id IN (?,?)", "ORDER BY aa.attempt_id, aa.question_id",
                "MIN(answer_id) AS answer_id");
    }
}
