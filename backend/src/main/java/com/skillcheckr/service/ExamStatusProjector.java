package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Exam;

/** Projects an exam's time-derived completion status without persisting it. */
public final class ExamStatusProjector {

    private ExamStatusProjector() {
    }

    public static Exam project(Exam exam, LocalDateTime now) {
        if (exam == null || exam.getDate() == null || exam.getEndTime() == null
                || !ExamConstants.isOpenForRegistration(exam.getStatus())) {
            return exam;
        }

        try {
            LocalDateTime windowEnd = LocalDateTime.parse(exam.getDate()).toLocalDate()
                    .atTime(exam.getEndTime());
            if (!now.isBefore(windowEnd)) {
                exam.setStatus(ExamConstants.EXAM_STATUS_COMPLETED);
            }
        } catch (DateTimeParseException ignored) {
            // An unreadable schedule must not invent a status transition.
        }
        return exam;
    }
}
