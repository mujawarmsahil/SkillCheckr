package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
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

    @Test
    void buildBatchMatchesSingleBuild() {
        Question question = question(7, ExamConstants.QUESTION_TYPE_MCQ);
        ExamQuestion examQuestion = ExamQuestion.builder().exam(Exam.builder().examId(10).build())
                .question(question).build();
        AttemptAnswer storedAnswer = AttemptAnswer.builder().attempt(ExamAttempt.builder().attemptId(20).build())
                .question(question).selectedAnswer(Answer.builder().optionText("B").correct(true).build())
                .correctOptionText("B").marksObtained(2).build();
        when(examQuestionRepository.findByExamIds(List.of(10))).thenReturn(List.of(examQuestion));
        when(examQuestionRepository.findByExamId(10)).thenReturn(List.of(assignment(question)));
        when(attemptAnswerRepository.findByAttemptIds(List.of(20))).thenReturn(List.of(storedAnswer));
        when(attemptAnswerRepository.findByAttemptId(20)).thenReturn(List.of(storedAnswer));

        Map<Integer, List<Map<String, Object>>> batch = service.buildBatch(List.of(10), List.of(20));

        assertThat(batch.get(20)).isEqualTo(service.build(10, 20));
        verify(examQuestionRepository).findByExamIds(List.of(10));
        verify(attemptAnswerRepository).findByAttemptIds(List.of(20));
    }

    @Test
    void buildBatchKeepsQuestionsAndAnswersIsolatedAcrossAttempts() {
        Question firstQuestion = question(7, ExamConstants.QUESTION_TYPE_MCQ);
        Question secondQuestion = question(8, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER);
        when(examQuestionRepository.findByExamIds(List.of(10, 11))).thenReturn(List.of(
                ExamQuestion.builder().exam(Exam.builder().examId(10).build()).question(firstQuestion).build(),
                ExamQuestion.builder().exam(Exam.builder().examId(11).build()).question(secondQuestion).build()));
        when(attemptAnswerRepository.findByAttemptIds(List.of(20, 21))).thenReturn(List.of(
                AttemptAnswer.builder().attempt(ExamAttempt.builder().attemptId(20).build())
                        .question(firstQuestion).selectedAnswer(Answer.builder().optionText("A").correct(true).build())
                        .correctOptionText("A").marksObtained(2).build(),
                AttemptAnswer.builder().attempt(ExamAttempt.builder().attemptId(21).build())
                        .question(secondQuestion).textAnswer("written").build()));

        Map<Integer, List<Map<String, Object>>> batch = service.buildBatch(List.of(10, 11), List.of(20, 21));

        assertThat(batch).containsOnlyKeys(20, 21);
        assertThat(batch.get(20)).singleElement().satisfies(item ->
                assertThat(item).containsEntry("selectedAnswer", "A").containsEntry("questionId", 7));
        assertThat(batch.get(21)).singleElement().satisfies(item ->
                assertThat(item).containsEntry("textAnswer", "written").containsEntry("questionId", 8));
    }

    @Test
    void buildBatchReturnsEmptyForEmptyOrMismatchedInputs() {
        assertThat(service.buildBatch(List.of(), List.of())).isEmpty();
        assertThat(service.buildBatch(List.of(10), List.of(20, 21))).isEmpty();

        verify(examQuestionRepository, never()).findByExamIds(org.mockito.ArgumentMatchers.anyList());
        verify(attemptAnswerRepository, never()).findByAttemptIds(org.mockito.ArgumentMatchers.anyList());
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
