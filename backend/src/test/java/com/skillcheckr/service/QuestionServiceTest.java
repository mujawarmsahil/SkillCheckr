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

import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Test
    void saveQuestionsWithAnswers_forwardsListToRepository() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("2+2?");
        dto.setCorrectOption("4");
        dto.setSubjectId(1);

        questionService.saveQuestionsWithAnswers(List.of(dto));

        verify(questionRepository).saveQuestionWithAnswers(List.of(dto));
    }

    @Test
    void saveQuestionsWithAnswers_handlesEmptyList() {
        questionService.saveQuestionsWithAnswers(List.of());

        verify(questionRepository).saveQuestionWithAnswers(List.of());
    }

    @Test
    void getQuestionsBySubjectId_delegatesToRepository() {
        QuestionDTO dto = new QuestionDTO();
        when(questionRepository.getQuestionsBySubjectId(1)).thenReturn(List.of(dto));

        List<QuestionDTO> actual = questionService.getQuestionsBySubjectId(1);

        assertThat(actual).hasSize(1);
        verify(questionRepository).getQuestionsBySubjectId(1);
    }

    @Test
    void getQuestionsByExamId_delegatesToRepository() {
        QuestionDTO dto = new QuestionDTO();
        when(questionRepository.getQuestionsByExamId(2)).thenReturn(List.of(dto));

        List<QuestionDTO> actual = questionService.getQuestionsByExamId(2);

        assertThat(actual).hasSize(1);
        verify(questionRepository).getQuestionsByExamId(2);
    }

    @Test
    void deleteQuestionById_delegatesToRepository() {
        when(questionRepository.deleteQuestionById(3)).thenReturn(true);

        assertThat(questionService.deleteQuestionById(3)).isTrue();
        verify(questionRepository).deleteQuestionById(3);
    }
}
