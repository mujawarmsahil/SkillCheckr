package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.ExamResultDTO;
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

    private ExamSubmissionServiceImpl service;
    private ExamAttempt attempt;
    private Exam exam;

    @BeforeEach
    void setUp() {
        service = new ExamSubmissionServiceImpl(examRepository, examAttemptRepository,
                examQuestionRepository, attemptAnswerRepository, resultRepository);
        exam = Exam.builder().examId(10).examName("Math").examType("MCQ")
                .totalMarks(10).passingMarks(4).build();
        attempt = ExamAttempt.builder().attemptId(1).exam(exam)
                .student(Student.builder().studentId(20).build())
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build();
        when(examAttemptRepository.findByIdForUpdate(1)).thenReturn(attempt);
        when(examRepository.getExamById(10)).thenReturn(exam);
        when(examQuestionRepository.findByExamId(10)).thenReturn(List.of(
                assignment(100, "MCQ", 5), assignment(101, "MCQ", 5)));
        when(attemptAnswerRepository.findByAttemptId(1)).thenReturn(List.of(
                answer(100, true), answer(101, false)));
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
        ExamResultDTO result = service.submit(10, 1, 20);

        assertThat(result.getResultId()).isEqualTo(50);
        assertThat(result.getMarksObtained()).isEqualTo(5);
        assertThat(result.getTotalMarks()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo("Pass");
        verify(attemptAnswerRepository).updateMarks(1, 100, 5);
        verify(attemptAnswerRepository).updateMarks(1, 101, 0);
        verify(examAttemptRepository).finalizeAttempt(any(Integer.class), any(LocalDateTime.class));
    }

    @Test
    void mixedExamLeavesTextMarksNullAndUsesPendingStatus() {
        exam.setExamType("MIXED");
        when(examQuestionRepository.findByExamId(10)).thenReturn(List.of(
                assignment(100, "MCQ", 5), assignment(102, "QUESTION_ANSWER", 5)));
        when(attemptAnswerRepository.findByAttemptId(1)).thenReturn(List.of(
                answer(100, true), AttemptAnswer.builder()
                        .question(Question.builder().questionId(102).build()).textAnswer("answer").build()));

        ExamResultDTO result = service.submit(10, 1, 20);

        assertThat(result.getMarksObtained()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo("Submitted for Evaluation");
        verify(attemptAnswerRepository).updateMarks(1, 102, null);
    }

    @Test
    void repeatedSubmissionReturnsExistingResultWithoutReprocessing() {
        ExamResultDTO existing = ExamResultDTO.builder().resultId(88).attemptId(1).status("Pass").build();
        attempt.setStatus("SUBMITTED");
        when(resultRepository.findByAttemptId(1)).thenReturn(existing);

        assertThat(service.submit(10, 1, 20)).isSameAs(existing);
        verify(examQuestionRepository, never()).findByExamId(10);
        verify(resultRepository, never()).insertSubmissionResult(any());
    }

    @Test
    void rejectsUnauthorizedStudent() {
        assertThatThrownBy(() -> service.submit(10, 1, 99))
                .isInstanceOf(ExamSubmissionException.class)
                .hasMessage("You are not authorized to submit this attempt");
    }

    @Test
    void expiredInProgressAttemptStillFinalizesUsingPersistedAnswers() {
        attempt.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        ExamResultDTO result = service.submit(10, 1, 20);

        assertThat(result.getResultId()).isEqualTo(50);
        verify(examAttemptRepository).finalizeAttempt(any(Integer.class), any(LocalDateTime.class));
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
