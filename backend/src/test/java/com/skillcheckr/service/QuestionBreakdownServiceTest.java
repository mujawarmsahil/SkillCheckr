package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamQuestionRepository;

@ExtendWith(MockitoExtension.class)
class QuestionBreakdownServiceTest {

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @InjectMocks
    private QuestionBreakdownService service;

    @Test
    void buildIncludesObjectiveAndDescriptiveAnswerStates() {
        Question correctMcq = question(1, ExamConstants.QUESTION_TYPE_MCQ);
        Question incorrectMcq = question(2, ExamConstants.QUESTION_TYPE_MCQ);
        Question unansweredMcq = question(3, ExamConstants.QUESTION_TYPE_MCQ);
        Question descriptive = question(4, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER);
        when(examQuestionRepository.findByExamId(10)).thenReturn(List.of(
                assignment(correctMcq), assignment(incorrectMcq), assignment(unansweredMcq), assignment(descriptive)));
        when(attemptAnswerRepository.findByAttemptId(20)).thenReturn(List.of(
                answer(correctMcq, Answer.builder().optionText("A").correct(true).build(), null, 2),
                answer(incorrectMcq, Answer.builder().optionText("B").correct(false).build(), null, 0),
                answer(descriptive, null, "Written response", null)));

        List<Map<String, Object>> breakdown = service.build(10, 20);

        assertThat(breakdown).hasSize(4);
        assertThat(breakdown.get(0)).containsEntry("selectedAnswer", "A").containsEntry("isCorrect", true);
        assertThat(breakdown.get(1)).containsEntry("selectedAnswer", "B").containsEntry("isCorrect", false);
        assertThat(breakdown.get(2)).containsEntry("selectedAnswer", null).containsEntry("isCorrect", false);
        assertThat(breakdown.get(3)).containsEntry("textAnswer", "Written response")
                .containsEntry("requiresEvaluation", true)
                .containsEntry("evaluated", false)
                .containsEntry("isCorrect", null);
    }

    @Test
    void buildReturnsEmptyWhenIdentifiersAreInvalid() {
        assertThat(service.build(0, 20)).isEmpty();
        assertThat(service.build(10, 0)).isEmpty();
    }

    private Question question(int id, String type) {
        return Question.builder().questionId(id).questionText("Question " + id).questionType(type).marks(2).build();
    }

    private ExamQuestion assignment(Question question) {
        return ExamQuestion.builder().question(question).build();
    }

    private AttemptAnswer answer(Question question, Answer selected, String text, Integer marks) {
        return AttemptAnswer.builder()
                .question(question)
                .selectedAnswer(selected)
                .textAnswer(text)
                .marksObtained(marks)
                .correctOptionText(selected != null && selected.isCorrect() ? selected.getOptionText() : "A")
                .build();
    }
}
