package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.exception.AttemptAnswerException;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.Student;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptAnswerServiceTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    private AttemptAnswerServiceImpl service;
    private ExamAttempt attempt;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2030, 5, 1, 12, 0);
        clock = new MutableClock(now);
        service = new AttemptAnswerServiceImpl(examAttemptRepository, examQuestionRepository, examRepository,
                questionRepository, attemptAnswerRepository, new AttemptExpiryPolicy(), clock);
        Exam exam = Exam.builder().examId(10).date(now.toLocalDate() + "T00:00")
                .startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(14, 0)).build();
        when(examRepository.getExamById(10)).thenReturn(Optional.of(exam));
        attempt = ExamAttempt.builder()
                .attemptId(1)
                .exam(Exam.builder().examId(10).build())
                .student(Student.builder().studentId(20).build())
                .startedAt(now.minusMinutes(1))
                .expiresAt(now.plusMinutes(30))
                .status("IN_PROGRESS")
                .build();
        when(examAttemptRepository.findByIdForUpdate(1)).thenReturn(attempt);
        when(examQuestionRepository.isQuestionAssigned(10, 100)).thenReturn(true);
        when(attemptAnswerRepository.saveAndReturn(any(AttemptAnswer.class))).thenAnswer(invocation -> {
            AttemptAnswer answer = invocation.getArgument(0);
            answer.setAttemptAnswerId(7);
            return answer;
        });
    }

    @Test
    void savesMcqAnswerWithoutCalculatingMarks() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));
        when(attemptAnswerRepository.answerBelongsToQuestion(12, 100)).thenReturn(true);

        var response = service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest(12, null));

        assertThat(response.getSelectedAnswerId()).isEqualTo(12);
        assertThat(response.getTextAnswer()).isNull();
        verify(attemptAnswerRepository).saveAndReturn(any(AttemptAnswer.class));
        verify(examAttemptRepository).findByIdForUpdate(1);
        verify(examAttemptRepository, never()).findById(1);
    }

    @Test
    void acceptsEmptyRequestAndPersistsBlankRow() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));

        var response = service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest());

        assertThat(response.getAttemptAnswerId()).isEqualTo(7);
        assertThat(response.getSelectedAnswerId()).isNull();
        assertThat(response.getTextAnswer()).isNull();
    }

    @Test
    void trimsTextAndStoresWhitespaceOnlyAsNull() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("QUESTION_ANSWER").wordLimit(3).build()));

        var response = service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest(null, "  "));
        assertThat(response.getTextAnswer()).isNull();

        response = service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest(null, " one   two "));
        assertThat(response.getTextAnswer()).isEqualTo("one   two");
    }

    @Test
    void rejectsInvalidTypeAndWordLimit() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("QUESTION_ANSWER").wordLimit(2).build()));

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20,
                new AttemptAnswerRequest(12, null)))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("selectedAnswerId is not valid for text questions");

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20,
                new AttemptAnswerRequest(null, "one two three")))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Text answer exceeds the allowed word limit");
    }

    @Test
    void rejectsOwnershipAndExpiredAttemptBeforeQuestionValidation() {
        attempt.setStudent(Student.builder().studentId(99).build());
        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("You are not authorized to modify this attempt");

        attempt.setStudent(Student.builder().studentId(20).build());
        attempt.setExpiresAt(clock.now().minusSeconds(1));
        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has expired");
    }

    @Test
    void rejectsSubmittedAttemptWithoutWritingAnAnswer() {
        attempt.setStatus("SUBMITTED");

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has already been submitted");

        verify(attemptAnswerRepository, never()).saveAndReturn(any());
    }

    @Test
    void rejectsAnswerAfterScheduledExamEndEvenWhenStoredExpiryIsLater() {
        Exam exam = Exam.builder().examId(10).date(clock.now().toLocalDate() + "T00:00")
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
        when(examRepository.getExamById(10)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has expired");

        verify(attemptAnswerRepository, never()).saveAndReturn(any());
    }

    @Test
    void rejectsAnswerExactlyAtAttemptExpiryWithoutPersisting() {
        clock.setNow(attempt.getExpiresAt());
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has expired");

        verify(attemptAnswerRepository, never()).saveAndReturn(any());
    }

    @Test
    void rejectsAnswerAfterAttemptExpiryWithoutPersisting() {
        clock.setNow(attempt.getExpiresAt().plusNanos(1));
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest()))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has expired");

        verify(attemptAnswerRepository, never()).saveAndReturn(any());
    }

    @Test
    void rechecksExpiryImmediatelyBeforePersistingAnswer() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));
        when(attemptAnswerRepository.answerBelongsToQuestion(12, 100)).thenAnswer(invocation -> {
            clock.setNow(attempt.getExpiresAt());
            return true;
        });

        assertThatThrownBy(() -> service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest(12, null)))
                .isInstanceOf(AttemptAnswerException.class)
                .hasMessage("Exam attempt has expired");

        verify(attemptAnswerRepository, never()).saveAndReturn(any());
    }

    @Test
    void acceptsAnswerBeforeAttemptExpiry() {
        when(questionRepository.findQuestionDetailsById(100))
                .thenReturn(Optional.of(Question.builder().questionId(100).questionType("MCQ").build()));
        when(attemptAnswerRepository.answerBelongsToQuestion(12, 100)).thenReturn(true);

        var response = service.saveAnswer(10, 1, 100, 20, new AttemptAnswerRequest(12, null));

        assertThat(response.getSelectedAnswerId()).isEqualTo(12);
        verify(attemptAnswerRepository).saveAndReturn(any(AttemptAnswer.class));
    }

    private static final class MutableClock extends Clock {
        private LocalDateTime now;
        private final ZoneId zone;

        private MutableClock(LocalDateTime now) {
            this(now, ZoneId.of("UTC"));
        }

        private MutableClock(LocalDateTime now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        private void setNow(LocalDateTime instant) {
            this.now = instant;
        }

        private LocalDateTime now() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(now, zone);
        }

        @Override
        public Instant instant() {
            return now.atZone(zone).toInstant();
        }
    }
}
