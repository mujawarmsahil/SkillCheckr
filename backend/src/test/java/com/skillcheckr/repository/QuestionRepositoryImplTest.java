package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessResourceFailureException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.exception.BadRequestException;
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
    void saveQuestionWithAnswers_rejectsAQuestionWithoutText() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("  ");

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Every question needs question text");
        verify(jdbcTemplate, never()).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }

    @Test
    void saveQuestionWithAnswers_rejectsAQuestionWithoutSubject() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("What is 2+2?");

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Every question needs a subject");
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

        // The generated key is returned for every insert, so the answer rows are written
        // against the id the database would have produced.
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(KeyHolder.class)))
                .thenAnswer(invocation -> {
                    KeyHolder kh = invocation.getArgument(1);
                    kh.getKeyList().add(Map.of("GENERATED_KEY", 101L));
                    return 1;
                });

        repository.saveQuestionWithAnswers(List.of(mcq, subj));

        verify(jdbcTemplate).update(eq("INSERT INTO answer (question_id, option_text, is_correct) VALUES (?, ?, ?)"),
                eq(101), eq("3"), eq(false));
        verify(jdbcTemplate).update(eq("INSERT INTO answer (question_id, option_text, is_correct) VALUES (?, ?, ?)"),
                eq(101), eq("4"), eq(true));
        verify(jdbcTemplate).update(eq("INSERT INTO answer (question_id, option_text, is_correct) VALUES (?, ?, ?)"),
                eq(101), eq("Polymorphism is many forms"), eq(true));
    }

    @Test
    void saveQuestionWithAnswers_rejectsAQuestionWhoseSubjectDiffersFromTheExam() {
        QuestionDTO dto = new QuestionDTO();
        dto.setSubjectId(2);
        dto.setQuestion("What is 2+2?");
        dto.setOption1("3");
        dto.setOption2("4");
        dto.setCorrectOption("4");
        dto.setExamId(5);
        when(jdbcTemplate.query(eq("SELECT subject_id FROM exam WHERE exam_id = ?"), any(RowMapper.class), eq(5)))
                .thenReturn(List.of(1));

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The question subject must match the exam subject");
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
        when(jdbcTemplate.queryForList(eq("SELECT question_type, marks, word_limit FROM question WHERE question_id = ?"), eq(1)))
                .thenReturn(List.of());
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
    void getQuestionsBySubjectId_propagatesDatabaseFailure() {
        when(jdbcTemplate.queryForList(anyString(), eq(10)))
                .thenThrow(new DataAccessResourceFailureException("SQL Error"));

        assertThatThrownBy(() -> repository.getQuestionsBySubjectId(10))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getQuestionsByExamId_returnsQuestionsInAssignmentOrder() {
        Map<String, Object> qRow = new HashMap<>();
        qRow.put("question_id", 1);
        qRow.put("subject_id", 10);
        qRow.put("question_text", "Sample Q");
        qRow.put("question_type", "MCQ");
        qRow.put("marks", 2);
        qRow.put("subject_name", "Maths");
        qRow.put("question_order", 1);

        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id"), eq(5))).thenReturn(List.of(qRow));
        when(jdbcTemplate.queryForList(startsWith("SELECT answer_id"), eq(1))).thenReturn(List.of());

        List<QuestionDTO> questions = repository.getQuestionsByExamId(5);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getExamId()).isEqualTo(5);
        assertThat(questions.get(0).getSubjectId()).isEqualTo(10);
        assertThat(questions.get(0).getSubjectName()).isEqualTo("Maths");
        assertThat(questions.get(0).getMarks()).isEqualTo(2);
    }

    @Test
    void getQuestionsByExamId_propagatesDatabaseFailure() {
        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id"), eq(5)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionsByExamId(5))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void deleteQuestionById_returnsTrue_whenRowsDeleted() {
        when(jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", 1)).thenReturn(4);
        when(jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", 1)).thenReturn(1);

        assertThat(repository.deleteQuestionById(1)).isTrue();
    }

    @Test
    void deleteQuestionById_propagatesDatabaseFailure() {
        when(jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", 1))
                .thenThrow(new DataAccessResourceFailureException("FK Error"));

        assertThatThrownBy(() -> repository.deleteQuestionById(1))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getAllQuestions_propagatesDatabaseFailureInsteadOfReturningAnEmptyList() {
        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id")))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAllQuestions())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getQuestionById_returnsEmpty_whenTheQuestionDoesNotExist() {
        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id"), eq(404))).thenReturn(List.of());

        assertThat(repository.getQuestionById(404)).isEmpty();
    }

    @Test
    void getQuestionById_propagatesDatabaseFailureInsteadOfHidingItAsAMissingQuestion() {
        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id"), eq(1)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionById(1))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void updateQuestion_propagatesDatabaseFailureInsteadOfReturningFalse() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(1);
        dto.setQuestion("What is Java?");
        dto.setSubjectId(10);

        when(jdbcTemplate.update(eq("UPDATE question SET question_text = ?, subject_id = ?, question_type = ?, marks = ?, word_limit = ? WHERE question_id = ?"),
                eq("What is Java?"), eq(10), eq("MCQ"), eq(1), any(), eq(1)))
                .thenThrow(new DataAccessResourceFailureException("read only"));

        assertThatThrownBy(() -> repository.updateQuestion(dto))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void loadQuestionMetadata_propagatesDatabaseFailureForOptionalColumns() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(1);
        dto.setQuestion("What is Java?");
        dto.setSubjectId(10);

        when(jdbcTemplate.queryForList(startsWith("SELECT q.question_id")))
                .thenReturn(List.of(Map.of("question_id", 1, "question_text", "What is Java?")));
        when(jdbcTemplate.queryForList(eq("SELECT question_type, marks, word_limit FROM question WHERE question_id = ?"), eq(1)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAllQuestions())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
