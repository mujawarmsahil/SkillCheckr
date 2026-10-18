package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Exam;

class ExamStatusProjectorTest {

    @Test
    void leavesAnUpcomingExamUpcomingAtItsWindowStart() {
        Exam exam = scheduledExam();

        ExamStatusProjector.project(exam, LocalDateTime.parse("2030-01-15T09:00:00"));

        assertThat(exam.getStatus()).isEqualTo(ExamConstants.EXAM_STATUS_UPCOMING);
    }

    @Test
    void marksAnExamCompletedAtItsWindowEnd() {
        Exam exam = scheduledExam();

        ExamStatusProjector.project(exam, LocalDateTime.parse("2030-01-15T10:30:00"));

        assertThat(exam.getStatus()).isEqualTo(ExamConstants.EXAM_STATUS_COMPLETED);
    }

    @Test
    void leavesAnExamUnchangedWhenItsScheduleCannotBeRead() {
        Exam exam = scheduledExam();
        exam.setDate("invalid");

        ExamStatusProjector.project(exam, LocalDateTime.parse("2030-01-15T11:00:00"));

        assertThat(exam.getStatus()).isEqualTo(ExamConstants.EXAM_STATUS_UPCOMING);
    }

    private Exam scheduledExam() {
        Exam exam = new Exam();
        exam.setDate("2030-01-15T09:00:00");
        exam.setEndTime(LocalTime.of(10, 30));
        exam.setStatus(ExamConstants.EXAM_STATUS_UPCOMING);
        return exam;
    }
}
