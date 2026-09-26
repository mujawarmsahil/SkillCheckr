package com.skillcheckr.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.Exam;

public final class ExamCreationValidator {

    private ExamCreationValidator() {}

    public static void validateExamForCreation(Exam exam) {
        validateExamForCreation(exam, LocalDate.now());
    }

    public static void validateExamForCreation(Exam exam, LocalDate today) {
        if (exam == null) {
            throw new BadRequestException("Invalid exam data");
        }
        validateExamName(exam.getExamName());
        validateExamDate(exam.getDate(), today);
    }

    public static void validateExamName(String examName) {
        if (examName == null || examName.isBlank()) {
            throw new BadRequestException("Exam name is required");
        }
        if (!examName.equals(examName.trim())) {
            throw new BadRequestException("Exam name must not start or end with a space");
        }
        if (!examName.matches(ExamConstants.EXAM_NAME_PATTERN)) {
            throw new BadRequestException(
                    "Exam name may only contain letters, single spaces and hyphens");
        }
    }

    public static void validateExamDate(String examDate, LocalDate today) {
        LocalDate date = parseDatePart(examDate);
        // An absent or unreadable date is not a scheduling rule violation; the
        // repository already rejects those when the row is written.
        if (date != null && date.isBefore(today.plusDays(ExamConstants.EXAM_MIN_LEAD_TIME_DAYS))) {
            throw new BadRequestException(
                    "Exam date must be at least " + ExamConstants.EXAM_MIN_LEAD_TIME_DAYS + " days from today");
        }
    }

    private static LocalDate parseDatePart(String examDate) {
        if (examDate == null || examDate.isBlank()) {
            return null;
        }
        // Exam date is stored as an ISO date-time string, e.g. "2026-09-25T10:00".
        String datePart = examDate.trim().split("[ T]")[0];
        try {
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
