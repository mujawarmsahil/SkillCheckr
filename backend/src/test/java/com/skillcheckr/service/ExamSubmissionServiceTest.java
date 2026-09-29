package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.EvaluationItemDTO;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.Student;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.ResultRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamSubmissionServiceTest {

    private static final int EXAM_ID = 10;
    private static final int ATTEMPT_ID = 1;
    private static final int STUDENT_ID = 20;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private QuestionBreakdownService questionBreakdownService;

    private ExamSubmissionServiceImpl service;
    private ExamAttempt attempt;
    private Exam exam;

    @BeforeEach
    void setUp() {
        service = new ExamSubmissionServiceImpl(examRepository, examAttemptRepository,
                examQuestionRepository, attemptAnswerRepository, resultRepository, questionBreakdownService);
        exam = Exam.builder().examId(EXAM_ID).examName("Math").examType(ExamConstants.QUESTION_TYPE_MCQ)
                .totalMarks(10).passingMarks(4).build();
        attempt = ExamAttempt.builder().attemptId(ATTEMPT_ID).exam(exam)
                .student(Student.builder().studentId(STUDENT_ID).build())
                .status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build();
        when(examAttemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(attempt);
        when(examRepository.getExamById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(101, ExamConstants.QUESTION_TYPE_MCQ, 5)));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                answer(100, true), answer(101, false)));
        when(questionBreakdownService.build(any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(Map.of("question_id", 100)));
        when(resultRepository.insertSubmissionResult(any(ExamResultDTO.class)))
                .thenAnswer(invocation -> {
                    ExamResultDTO result = invocation.getArgument(0);
                    result.setResultId(50);
                    return result;
                });
        when(examAttemptRepository.finalizeAttempt(any(Integer.class), any(LocalDateTime.class))).thenReturn(true);
    }

    @Test
    void scoresCorrectAndIncorrectMcqAndFinalizesAttempt() {
        ExamResultDTO result = service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID);

        assertThat(result.getResultId()).isEqualTo(50);
        assertThat(result.getMarksObtained()).isEqualTo(5);
        assertThat(result.getTotalMarks()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo(ExamConstants.RESULT_STATUS_PASS);
        verify(attemptAnswerRepository).updateMarks(ATTEMPT_ID, 100, 5);
        verify(attemptAnswerRepository).updateMarks(ATTEMPT_ID, 101, 0);
        verify(examAttemptRepository).finalizeAttempt(any(Integer.class), any(LocalDateTime.class));
    }

    @Test
    void submit_returnsTheServerDerivedBreakdownOnTheInsertedResult() {
        ExamResultDTO result = service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID);

        assertThat(result.getQuestionBreakdown()).hasSize(1);
        verify(questionBreakdownService).build(EXAM_ID, ATTEMPT_ID);
    }

    @Test
    void submit_failsWhenTheAttemptCannotBeFinalized() {
        when(examAttemptRepository.finalizeAttempt(any(Integer.class), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("Unable to finalize exam attempt");
    }

    @Test
    void mixedExamLeavesTextMarksNullAndUsesPendingStatus() {
        exam.setExamType("MIXED");
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                answer(100, true), AttemptAnswer.builder()
                        .question(Question.builder().questionId(102).build()).textAnswer("answer").build()));

        ExamResultDTO result = service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID);

        assertThat(result.getMarksObtained()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(ExamConstants.RESULT_STATUS_SUBMITTED_FOR_EVALUATION);
        verify(attemptAnswerRepository).updateMarks(ATTEMPT_ID, 102, null);
    }

    @Test
    void repeatedSubmissionReturnsExistingResultWithBreakdownAndWithoutReprocessing() {
        ExamResultDTO existing = ExamResultDTO.builder()
                .resultId(88).attemptId(ATTEMPT_ID).status(ExamConstants.RESULT_STATUS_PASS).build();
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(resultRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(existing));

        ExamResultDTO result = service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID);

        assertThat(result.getResultId()).isEqualTo(88);
        assertThat(result.getQuestionBreakdown()).hasSize(1);
        verify(examQuestionRepository, never()).findByExamId(EXAM_ID);
        verify(resultRepository, never()).insertSubmissionResult(any());
    }

    @Test
    void rejectsUnauthorizedStudent() {
        assertThatThrownBy(() -> service.submit(EXAM_ID, ATTEMPT_ID, 99))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("You are not authorized to submit this attempt");
    }

    @Test
    void rejectsAnAttemptFromAnotherExam() {
        assertThatThrownBy(() -> service.submit(11, ATTEMPT_ID, STUDENT_ID))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("Attempt does not belong to this exam");
    }

    @Test
    void expiredInProgressAttemptStillFinalizesUsingPersistedAnswers() {
        attempt.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        ExamResultDTO result = service.submit(EXAM_ID, ATTEMPT_ID, STUDENT_ID);

        assertThat(result.getResultId()).isEqualTo(50);
        verify(examAttemptRepository).finalizeAttempt(any(Integer.class), any(LocalDateTime.class));
    }

    @Test
    void listSubmissions_marksAttemptsThatStillNeedEvaluation() {
        when(resultRepository.findSubmissionSummariesByExamId(EXAM_ID)).thenReturn(List.of(
                ExamSubmissionSummaryDTO.builder().attemptId(1).build(),
                ExamSubmissionSummaryDTO.builder().attemptId(2).build()));
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));
        // Attempt 1 was already graded by a teacher, attempt 2 still waits for marks.
        when(attemptAnswerRepository.findByAttemptId(1)).thenReturn(List.of(
                answer(100, true),
                AttemptAnswer.builder().question(Question.builder().questionId(102).build())
                        .textAnswer("graded").marksObtained(4).build()));
        when(attemptAnswerRepository.findByAttemptId(2)).thenReturn(List.of(
                answer(100, true),
                AttemptAnswer.builder().question(Question.builder().questionId(102).build())
                        .textAnswer("not graded yet").build()));

        List<ExamSubmissionSummaryDTO> submissions = service.listSubmissions(EXAM_ID);

        assertThat(submissions).hasSize(2);
        assertThat(submissions.get(0).isPendingEvaluation()).isFalse();
        assertThat(submissions.get(1).isPendingEvaluation()).isTrue();
    }

    @Test
    void getEvaluationItems_exposesTheSavedTextAnswer() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                answer(100, true),
                AttemptAnswer.builder().question(Question.builder().questionId(102).build())
                        .textAnswer("my long answer").build()));

        List<EvaluationItemDTO> items = service.getEvaluationItems(EXAM_ID, ATTEMPT_ID);

        assertThat(items).hasSize(2);
        EvaluationItemDTO descriptive = items.stream()
                .filter(item -> item.getQuestionId() == 102).findFirst().orElseThrow();
        assertThat(descriptive.getTextAnswer()).isEqualTo("my long answer");
        assertThat(descriptive.isRequiresEvaluation()).isTrue();
        assertThat(descriptive.isEvaluated()).isFalse();
    }

    @Test
    void getEvaluationItems_refusesAnAttemptThatIsStillRunning() {
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);

        assertThatThrownBy(() -> service.getEvaluationItems(EXAM_ID, ATTEMPT_ID))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("This attempt has not been submitted yet");
    }

    @Test
    void awardAnswerMarks_recomputesTheResultAndClearsThePendingStatus() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(
                answer(100, true),
                AttemptAnswer.builder().question(Question.builder().questionId(102).build())
                        .textAnswer("graded").build(),
                AttemptAnswer.builder().question(Question.builder().questionId(102).build())
                        .textAnswer("graded").marksObtained(4).build()));
        when(resultRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(ExamResultDTO.builder()
                .resultId(50).marksObtained(9).status(ExamConstants.RESULT_STATUS_PASS).build()));

        ExamResultDTO result = service.awardAnswerMarks(EXAM_ID, ATTEMPT_ID, 102, 4);

        assertThat(result.getMarksObtained()).isEqualTo(9);
        assertThat(result.getQuestionBreakdown()).hasSize(1);
        verify(attemptAnswerRepository).updateMarks(ATTEMPT_ID, 102, 4);
        verify(resultRepository).updateResultAfterEvaluation(ATTEMPT_ID, 9, 10, 4, 90.0,
                ExamConstants.RESULT_STATUS_PASS);
    }

    @Test
    void awardAnswerMarks_recordsMarksForAQuestionTheStudentSkipped() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(answer(100, true)));
        when(resultRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(Optional.of(ExamResultDTO.builder()
                .resultId(50).marksObtained(5).status(ExamConstants.RESULT_STATUS_PASS).build()));

        service.awardAnswerMarks(EXAM_ID, ATTEMPT_ID, 102, 0);

        ArgumentCaptor<AttemptAnswer> captor = ArgumentCaptor.forClass(AttemptAnswer.class);
        verify(attemptAnswerRepository).saveAndReturn(captor.capture());
        assertThat(captor.getValue().getQuestion().getQuestionId()).isEqualTo(102);
        assertThat(captor.getValue().getMarksObtained()).isZero();
        assertThat(captor.getValue().getAttempt().getAttemptId()).isEqualTo(ATTEMPT_ID);
        verify(attemptAnswerRepository, never()).updateMarks(ATTEMPT_ID, 102, 0);
    }

    @Test
    void awardAnswerMarks_refusesToGradeAMultipleChoiceQuestion() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);

        assertThatThrownBy(() -> service.awardAnswerMarks(EXAM_ID, ATTEMPT_ID, 100, 5))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("Multiple choice answers are graded automatically");
    }

    @Test
    void awardAnswerMarks_rejectsMarksAboveTheQuestionValue() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);
        when(examQuestionRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
                assignment(100, ExamConstants.QUESTION_TYPE_MCQ, 5),
                assignment(102, ExamConstants.QUESTION_TYPE_QUESTION_ANSWER, 5)));

        assertThatThrownBy(() -> service.awardAnswerMarks(EXAM_ID, ATTEMPT_ID, 102, 99))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("Marks must be between 0 and 5");
    }

    @Test
    void awardAnswerMarks_refusesAQuestionThatIsNotPartOfTheExam() {
        attempt.setStatus(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
        when(examAttemptRepository.findById(ATTEMPT_ID)).thenReturn(attempt);

        assertThatThrownBy(() -> service.awardAnswerMarks(EXAM_ID, ATTEMPT_ID, 777, 1))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("Question 777 is not assigned to this exam");
    }

    private ExamQuestion assignment(int questionId, String type, int marks) {
        return ExamQuestion.builder()
                .question(Question.builder().questionId(questionId).questionType(type).marks(marks).build())
                .questionOrder(questionId)
                .build();
    }

    private AttemptAnswer answer(int questionId, boolean correct) {
        return AttemptAnswer.builder()
                .question(Question.builder().questionId(questionId).build())
                .selectedAnswer(Answer.builder().answerId(questionId + 1).correct(correct).build())
                .build();
    }
}
