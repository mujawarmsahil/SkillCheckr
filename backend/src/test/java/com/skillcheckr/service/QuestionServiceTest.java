package com.skillcheckr.service;

import static org.mockito.Mockito.verify;

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
}