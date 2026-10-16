package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;

class AttemptExpiryPolicyTest {

    private final AttemptExpiryPolicy policy = new AttemptExpiryPolicy();

    @Test
    void durationExpiryIsUsedWhenItFallsBeforeTheScheduledEnd() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        Exam exam = examRunningOn(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(16, 0));
        ExamAttempt attempt = attemptExpiringAt(expiresAt);

        assertThat(policy.effectiveExpiry(attempt, exam)).isEqualTo(expiresAt);
    }

    @Test
    void scheduledEndClampsAnAttemptThatOutlivesTheExamWindow() {
        LocalDate today = LocalDate.now();
        Exam exam = examRunningOn(today, LocalTime.of(9, 0), LocalTime.of(11, 0));
        // The student would still be working well after the scheduled window closed.
        ExamAttempt attempt = attemptExpiringAt(LocalDateTime.now().plusDays(2));

        assertThat(policy.effectiveExpiry(attempt, exam))
                .isEqualTo(LocalDateTime.of(today, LocalTime.of(11, 0)));
    }

    @Test
    void windowCrossingMidnightClampsToTheNextDayEnd() {
        LocalDate today = LocalDate.now();
        Exam exam = examRunningOn(today, LocalTime.of(23, 0), LocalTime.of(1, 0));
        ExamAttempt attempt = attemptExpiringAt(LocalDateTime.now().plusDays(2));

        assertThat(policy.effectiveExpiry(attempt, exam))
                .isEqualTo(LocalDateTime.of(today.plusDays(1), LocalTime.of(1, 0)));
    }

    @Test
    void unreadableAttemptWindowIsTreatedAsExpiredRatherThanOpen() {
        Exam exam = examRunningOn(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(16, 0));
        ExamAttempt attempt = attemptExpiringAt(LocalDateTime.now().plusHours(1));
        attempt.setExpiresAt(null);

        assertThat(policy.effectiveExpiry(attempt, exam)).isNull();
        assertThat(policy.isExpired(attempt, exam, LocalDateTime.now())).isTrue();
    }

    @Test
    void anAttemptIsNotExpiredOneMinuteBeforeItsEffectiveExpiry() {
        Exam exam = examRunningOn(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(16, 0));
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);
        ExamAttempt attempt = attemptExpiringAt(expiry);

        assertThat(policy.isExpired(attempt, exam, expiry.minusMinutes(1))).isFalse();
        assertThat(policy.isExpired(attempt, exam, expiry)).isTrue();
        assertThat(policy.isExpired(attempt, exam, expiry.plusMinutes(1))).isTrue();
    }

    @Test
    void anAttemptIsExpiredOnceTheScheduledEndHasPassed() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examRunningOn(now.toLocalDate().minusDays(1), LocalTime.of(9, 0), LocalTime.of(16, 0));
        ExamAttempt attempt = attemptExpiringAt(now.minusHours(3));

        assertThat(policy.isExpired(attempt, exam, now)).isTrue();
    }

    @Test
    void aClosedWindowExpiresAnAttemptEvenWhileTheStudentStillHasTimeLeft() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examRunningOn(now.toLocalDate().minusDays(1), LocalTime.of(9, 0), LocalTime.of(16, 0));
        ExamAttempt attempt = attemptExpiringAt(now.plusHours(3));

        assertThat(policy.effectiveExpiry(attempt, exam))
                .isEqualTo(LocalDateTime.of(now.toLocalDate().minusDays(1), LocalTime.of(16, 0)));
        assertThat(policy.isExpired(attempt, exam, now)).isTrue();
    }

    /**
     * The exam date is the wall-clock day the window runs on, and it is the only calendar
     * information the policy has. Scheduling the window on a fixed day keeps the assertion
     * independent of the wall clock at the moment the test runs.
     */
    private Exam examRunningOn(LocalDate date, LocalTime startTime, LocalTime endTime) {
        Exam exam = new Exam();
        exam.setExamId(1);
        exam.setDate(date.atTime(startTime).toString());
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        return exam;
    }

    private ExamAttempt attemptExpiringAt(LocalDateTime expiresAt) {
        return ExamAttempt.builder()
                .attemptId(1)
                .startedAt(expiresAt.minusHours(1))
                .expiresAt(expiresAt)
                .status("IN_PROGRESS")
                .build();
    }
}