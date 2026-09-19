package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.model.QuestionDTO;

@ExtendWith(MockitoExtension.class)
class QuestionRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private QuestionRepositoryImpl repository;

    @Test
    void saveQuestionWithAnswers_handlesNullAndEmptyList() {
        repository.saveQuestionWithAnswers(null);
        repository.saveQuestionWithAnswers(List.of());
    }

    @Test
    void saveQuestionWithAnswers_handlesBlankQuestion() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("  ");
        repository.saveQuestionWithAnswers(List.of(dto));
    }

    @Test
    void saveQuestionWithAnswers_insertsMcqAndSubjectiveQuestions() {
        QuestionDTO mcq = new QuestionDTO();
        mcq.setSubjectId(1);
        mcq.setQuestion("What is 2+2?");
        mcq.setOption1("3");
        mcq.setOption2("4");
        mcq.setOption3("5");
        mcq.setOption4("6");
        mcq.setCorrectOption("4");

        QuestionDTO subj = new QuestionDTO();
        subj.setSubjectId(1);
        subj.setQuestion("Explain polymorphism");
        subj.setSampleAnswer("Polymorphism is many forms");

        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class)))
                .thenAnswer(invocation -> {
                    KeyHolder kh = invocation.getArgument(1);
                    Map<String, Object> keyMap = new HashMap<>();
                    keyMap.put("GENERATED_KEY", 101);
                    kh.getKeyList().add(keyMap);
                    return 1;
                });

        repository.saveQuestionWithAnswers(List.of(mcq, subj));

        verify(jdbcTemplate).update(eq("INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)"),
                eq(101), eq("4"), eq(true));
        verify(jdbcTemplate).update(eq("INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)"),
                eq(101), eq("Polymorphism is many forms"), eq(true));
    }

    @Test
    void getQuestionsBySubjectId_returnsMcqQuestions() {
        Map<String, Object> qRow = new HashMap<>();
        qRow.put("question_id", 1);
        qRow.put("question_text", "What is Java?");

        Map<String, Object> a1 = new HashMap<>();
        a1.put("answer_id", 10);
        a1.put("option_text", "A language");
        a1.put("is_correct", true);

        Map<String, Object> a2 = new HashMap<>();
        a2.put("answer_id", 11);
        a2.put("option_text", "An island");
        a2.put("is_correct", 0);

        when(jdbcTemplate.queryForList(eq("SELECT question_id, subject_id, question_text FROM question WHERE subject_id = ?"), eq(10)))
                .thenReturn(List.of(qRow));
        when(jdbcTemplate.queryForList(eq("SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC"), eq(1)))
                .thenReturn(List.of(a1, a2));

        List<QuestionDTO> result = repository.getQuestionsBySubjectId(10);

        assertThat(result).hasSize(1);
        QuestionDTO dto = result.get(0);
        assertThat(dto.getQuestion()).isEqualTo("What is Java?");
        assertThat(dto.getQuestionType()).isEqualTo("MCQ");
        assertThat(dto.getOption1()).isEqualTo("A language");
        assertThat(dto.getOption2()).isEqualTo("An island");
        assertThat(dto.getOption1Id()).isEqualTo(10);
        assertThat(dto.getOption2Id()).isEqualTo(11);
        assertThat(dto.getCorrectOption()).isEqualTo("A language");
    }

    @Test
    void getQuestionsBySubjectId_returnsSubjectiveQuestions() {
        Map<String, Object> qRow = new HashMap<>();
        qRow.put("question_id", 2);
        qRow.put("question_text", "Describe encapsulation");

        Map<String, Object> a1 = new HashMap<>();
        a1.put("answer_id", 20);
        a1.put("option_text", "Data hiding");
        a1.put("is_correct", true);

        when(jdbcTemplate.queryForList(anyString(), eq(10))).thenReturn(List.of(qRow));
        when(jdbcTemplate.queryForList(anyString(), eq(2))).thenReturn(List.of(a1));

        List<QuestionDTO> result = repository.getQuestionsBySubjectId(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestionType()).isEqualTo("QUESTION_ANSWER");
        assertThat(result.get(0).getSampleAnswer()).isEqualTo("Data hiding");
    }

    @Test
    void getQuestionsBySubjectId_returnsEmptyList_onException() {
        when(jdbcTemplate.queryForList(anyString(), eq(10))).thenThrow(new RuntimeException("SQL Error"));

        List<QuestionDTO> result = repository.getQuestionsBySubjectId(10);

        assertThat(result).isEmpty();
    }

    @Test
    void getQuestionsByExamId_returnsQuestions_whenSubjectFound() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5))).thenReturn(10);

        Map<String, Object> qRow = new HashMap<>();
        qRow.put("question_id", 1);
        qRow.put("question_text", "Sample Q");

        when(jdbcTemplate.queryForList(eq("SELECT question_id, subject_id, question_text FROM question WHERE subject_id = ?"), eq(10)))
                .thenReturn(List.of(qRow));
        when(jdbcTemplate.queryForList(eq("SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC"), eq(1)))
                .thenReturn(List.of());

        List<QuestionDTO> questions = repository.getQuestionsByExamId(5);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getExamId()).isEqualTo(5);
    }

    @Test
    void getQuestionsByExamId_returnsEmpty_whenException() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5))).thenThrow(new RuntimeException("Not found"));

        List<QuestionDTO> questions = repository.getQuestionsByExamId(5);

        assertThat(questions).isEmpty();
    }

    @Test
    void deleteQuestionById_returnsTrue_whenRowsDeleted() {
        when(jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", 1)).thenReturn(4);
        when(jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", 1)).thenReturn(1);

        assertThat(repository.deleteQuestionById(1)).isTrue();
    }

    @Test
    void deleteQuestionById_returnsFalse_whenExceptionThrown() {
        when(jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", 1)).thenThrow(new RuntimeException("FK Error"));

        assertThat(repository.deleteQuestionById(1)).isFalse();
    }
}
