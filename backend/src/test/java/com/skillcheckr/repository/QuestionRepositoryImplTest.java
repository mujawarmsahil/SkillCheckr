package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.persistence.entity.QuestionEntity;
import com.skillcheckr.persistence.repository.AnswerJpaRepository;
import com.skillcheckr.persistence.repository.QuestionJpaRepository;

@ExtendWith(MockitoExtension.class)
class QuestionRepositoryImplTest {

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Mock
    private QuestionJpaRepository questionRepository;
    @Mock
    private AnswerJpaRepository answerRepository;

    @InjectMocks
    private QuestionRepositoryImpl repository;

    @Test
    void saveQuestionWithAnswers_handlesNullAndEmptyList() {
        repository.saveQuestionWithAnswers(null);
        repository.saveQuestionWithAnswers(List.of());
        verifyNoInteractions(questionRepository);
    }

    @Test
    void saveQuestionWithAnswers_rejectsAQuestionWithoutText() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("  ");

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Every question needs question text");
        verifyNoInteractions(questionRepository);
    }

    @Test
    void saveQuestionWithAnswers_rejectsAQuestionWithoutSubject() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("What is 2+2?");

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Every question needs a subject");
        verifyNoInteractions(questionRepository);
    }

    @Test
    void saveQuestionWithAnswers_rejectsAQuestionWhoseSubjectDiffersFromTheExam() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("What is 2+2?");
        dto.setSubjectId(1);
        dto.setExamId(5);
        when(questionRepository.findExamSubjectId(5)).thenReturn(List.of(2));

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The question subject must match the exam subject");
    }

    @Test
    void saveQuestionWithAnswers_throwsWhenExamDoesNotExist() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("What is 2+2?");
        dto.setSubjectId(1);
        dto.setExamId(5);
        when(questionRepository.findExamSubjectId(5)).thenReturn(List.of());

        assertThatThrownBy(() -> repository.saveQuestionWithAnswers(List.of(dto)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveQuestionWithAnswers_insertsMcqQuestion() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("What is 2+2?");
        dto.setSubjectId(1);
        dto.setOption1("4");
        dto.setCorrectOption("4");
        when(questionRepository.saveAndFlush(any(QuestionEntity.class))).thenAnswer(invocation -> {
            QuestionEntity e = invocation.getArgument(0);
            e.setQuestionId(7);
            return e;
        });

        repository.saveQuestionWithAnswers(List.of(dto));

        verify(questionRepository).saveAndFlush(any(QuestionEntity.class));
        verify(answerRepository).save(any(com.skillcheckr.persistence.entity.AnswerEntity.class));
    }

    @Test
    void getQuestionsBySubjectId_propagatesDatabaseFailure() {
        when(jdbcTemplate.queryForList(any(String.class), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionsBySubjectId(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getAllQuestions_propagatesDatabaseFailureInsteadOfReturningAnEmptyList() {
        when(jdbcTemplate.queryForList(any(String.class)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAllQuestions())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getQuestionById_returnsEmpty_whenTheQuestionDoesNotExist() {
        when(jdbcTemplate.queryForList(any(String.class), anyInt())).thenReturn(List.of());

        assertThat(repository.getQuestionById(9)).isEmpty();
    }

    @Test
    void getQuestionById_propagatesDatabaseFailureInsteadOfHidingItAsAMissingQuestion() {
        when(jdbcTemplate.queryForList(any(String.class), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionById(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void deleteQuestionById_deletesAnswersAndQuestion() {
        repository.deleteQuestionById(9);

        verify(answerRepository).deleteByQuestionId(9);
        verify(questionRepository).deleteExamQuestionLinksByQuestionId(9);
        verify(questionRepository).deleteById(9);
    }

    @Test
    void updateQuestion_returnsFalse_whenTheQuestionDoesNotExist() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(9);
        dto.setQuestion("updated");
        when(questionRepository.findById(9)).thenReturn(Optional.empty());

        assertThat(repository.updateQuestion(dto)).isFalse();
    }
}
