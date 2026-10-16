package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;

class AnswerKeyReleasePolicyTest {

    private final AnswerKeyReleasePolicy policy = new AnswerKeyReleasePolicy();

    @Test
    void aWindowThatClosedYesterdayReleasesTheAnswerKey() {
        Exam exam = examRunningAt(LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(policy.isReleased(exam)).isTrue();
    }

    @Test
    void aWindowThatHasNotClosedYetKeepsTheAnswerKeyHidden() {
        // Scheduled so that even in the last hour before midnight the window is still open.
        Exam exam = examRunningAt(LocalDate.now(), LocalTime.of(0, 0), LocalTime.of(23, 59));

        assertThat(policy.isReleased(exam)).isFalse();
    }

    @Test
    void aFutureWindowKeepsTheAnswerKeyHidden() {
        Exam exam = examRunningAt(LocalDate.now().plusDays(30), LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(policy.isReleased(exam)).isFalse();
    }

    @Test
    void anUnreleasedResultExposesTheScoreButNoPerQuestionAnswers() {
        Exam exam = examRunningAt(LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));
        ExamResultDTO result = resultWithBreakdown();

        policy.protectStudentResult(result, exam);

        assertThat(result.getAnswerKeyReleased()).isFalse();
        assertThat(result.getQuestionBreakdown()).isEmpty();
        assertThat(result.getMarksObtained()).isEqualTo(4);
    }

    @Test
    void aReleasedResultKeepsThePerQuestionAnswers() {
        Exam exam = examRunningAt(LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        ExamResultDTO result = resultWithBreakdown();

        policy.protectStudentResult(result, exam);

        assertThat(result.getAnswerKeyReleased()).isTrue();
        assertThat(result.getQuestionBreakdown()).hasSize(1);
    }

    private ExamResultDTO resultWithBreakdown() {
        ExamResultDTO result = new ExamResultDTO();
        result.setMarksObtained(4);
        result.setQuestionBreakdown(java.util.List.of(java.util.Map.of("questionId", 1, "correctAnswer", "B")));
        return result;
    }

    /**
     * The release rule is "the scheduled window has closed", so the assertions pin the
     * window to whole days relative to today instead of to the current time of day.
     */
    private Exam examRunningAt(LocalDate date, LocalTime startTime, LocalTime endTime) {
        Exam exam = new Exam();
        exam.setExamId(1);
        exam.setDate(date.atTime(startTime).toString());
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        return exam;
    }
}