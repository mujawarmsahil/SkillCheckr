package com.skillcheckr.validation;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        validateSchedule(exam);
        validateMarks(exam);
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

    public static void validateSchedule(Exam exam) {
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            throw new BadRequestException("Exam start and end times are required");
        }
        if (exam.getDurationMinutes() <= 0) {
            throw new BadRequestException("Exam duration must be at least 1 minute");
        }
        if (exam.getDurationMinutes() > ExamConstants.EXAM_MAX_DURATION_MINUTES) {
            throw new BadRequestException(
                    "Exam duration must not exceed " + ExamConstants.EXAM_MAX_DURATION_MINUTES + " minutes");
        }
        if (!exam.getEndTime().isAfter(exam.getStartTime())) {
            throw new BadRequestException("Exam end time must be after the start time");
        }

        long windowMinutes = Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
        if (exam.getDurationMinutes() > windowMinutes) {
            throw new BadRequestException(
                    "Exam duration cannot be longer than the scheduled exam window");
        }
    }

    public static void validateMarks(Exam exam) {
        if (exam.getTotalMarks() <= 0) {
            throw new BadRequestException("Total marks must be greater than 0");
        }
        if (exam.getPassingMarks() <= 0) {
            throw new BadRequestException("Passing marks must be greater than 0");
        }
        if (exam.getPassingMarks() > exam.getTotalMarks()) {
            throw new BadRequestException("Passing marks cannot exceed the total marks");
        }
    }

    /**
     * Guards the exam lifecycle: a completed exam can no longer change status and only
     * an administrator may open, close or re-open an exam.
     */
    public static void validateStatusChange(Exam exam, String newStatus) {
        if (exam == null) {
            throw new BadRequestException("Exam not found");
        }
        if (newStatus == null || newStatus.isBlank()) {
            throw new BadRequestException("A valid exam status is required");
        }
        if (!ExamConstants.ADMIN_SETTABLE_EXAM_STATUSES.stream()
                .anyMatch(status -> status.equalsIgnoreCase(newStatus))) {
            throw new BadRequestException("'" + newStatus.trim() + "' is not a valid exam status");
        }
        if (ExamConstants.EXAM_STATUS_COMPLETED.equalsIgnoreCase(exam.getStatus())) {
            throw new BadRequestException("A completed exam can no longer change status");
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

    /**
     * Resolves the moment an exam window opens and closes, treating an end time earlier
     * than the start time as a window that runs past midnight.
     */
    public static LocalDateTime resolveWindowStart(Exam exam) {
        return LocalDateTime.of(parseDatePart(exam.getDate()), asTime(exam.getStartTime()));
    }

    public static LocalDateTime resolveWindowEnd(Exam exam) {
        LocalDate date = parseDatePart(exam.getDate());
        LocalTime start = asTime(exam.getStartTime());
        LocalTime end = asTime(exam.getEndTime());
        LocalDateTime endDateTime = LocalDateTime.of(date, end);
        return !endDateTime.isAfter(LocalDateTime.of(date, start)) ? endDateTime.plusDays(1) : endDateTime;
    }

    private static LocalTime asTime(LocalTime time) {
        return time == null ? LocalTime.MIDNIGHT : time;
    }
}
