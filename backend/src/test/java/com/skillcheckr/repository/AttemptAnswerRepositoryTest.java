package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

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
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            KeyHolder keyHolder = invocation.getArgument(1);
            creator.createPreparedStatement(connection);
            keyHolder.getKeyList().add(Map.of("attempt_answer_id", 42L));
            return 1;
        }).when(jdbcTemplate).update(org.mockito.ArgumentMatchers.any(PreparedStatementCreator.class),
                org.mockito.ArgumentMatchers.any(KeyHolder.class));
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
        verify(connection).prepareStatement(sql.capture(), eq(Statement.RETURN_GENERATED_KEYS));
        assertThat(sql.getValue()).contains("attempt_answer_id = LAST_INSERT_ID(attempt_answer_id)");
    }
}
