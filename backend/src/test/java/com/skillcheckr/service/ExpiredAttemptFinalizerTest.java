package com.skillcheckr.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Student;
import com.skillcheckr.repository.ExamAttemptRepository;

@ExtendWith(MockitoExtension.class)
class ExpiredAttemptFinalizerTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamSubmissionService examSubmissionService;

    private final AttemptExpiryPolicy attemptExpiryPolicy = new AttemptExpiryPolicy();

    @Test
    void finalizesExpiredAndAbandonedAttemptsButLeavesLiveAttemptsOpen() {
        LocalDateTime now = LocalDateTime.now();
        Exam endedExam = examAt(1, now.minusHours(2), now.minusHours(1));
        Exam liveExam = examAt(2, now.minusMinutes(30), now.plusMinutes(30));
        ExamAttempt abandoned = attempt(11, endedExam, now.plusMinutes(20));
        ExamAttempt live = attempt(12, liveExam, now.plusMinutes(20));
        when(examAttemptRepository.findInProgressAttemptsAfterId(0, 250))
                .thenReturn(List.of(abandoned, live));
        new ExpiredAttemptFinalizer(examAttemptRepository, examSubmissionService, attemptExpiryPolicy)
                .finalizeExpiredAttempts();

        verify(examSubmissionService).submit(1, 11, 7);
    }

    @Test
    void scansAllPagesOfInProgressAttempts() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examAt(1, now.minusHours(2), now.minusHours(1));
        List<ExamAttempt> firstPage = IntStream.rangeClosed(1, 250)
                .mapToObj(id -> attempt(id, exam, now.plusMinutes(5)))
                .toList();
        when(examAttemptRepository.findInProgressAttemptsAfterId(0, 250))
                .thenReturn(firstPage);
        when(examAttemptRepository.findInProgressAttemptsAfterId(250, 250)).thenReturn(List.of());

        new ExpiredAttemptFinalizer(examAttemptRepository, examSubmissionService, attemptExpiryPolicy)
                .finalizeExpiredAttempts();

        ArgumentCaptor<Integer> attemptIds = ArgumentCaptor.forClass(Integer.class);
        verify(examSubmissionService, org.mockito.Mockito.times(250)).submit(
                org.mockito.ArgumentMatchers.eq(1), attemptIds.capture(), org.mockito.ArgumentMatchers.eq(7));
        assertThat(attemptIds.getAllValues()).containsExactlyElementsOf(
                IntStream.rangeClosed(1, 250).boxed().toList());
        verify(examAttemptRepository).findInProgressAttemptsAfterId(250, 250);
    }

    private Exam examAt(int examId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return Exam.builder().examId(examId).date(startsAt.toLocalDate() + "T" + startsAt.toLocalTime())
                .startTime(startsAt.toLocalTime()).endTime(endsAt.toLocalTime()).build();
    }

    private ExamAttempt attempt(int attemptId, Exam exam, LocalDateTime expiresAt) {
        return ExamAttempt.builder().attemptId(attemptId).exam(exam)
                .student(Student.builder().studentId(7).build()).expiresAt(expiresAt)
                .status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS).build();
    }
}
