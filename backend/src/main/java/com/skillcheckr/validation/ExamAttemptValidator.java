package com.skillcheckr.validation;

import java.time.LocalDateTime;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptAnswerException;
import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.model.ExamAttempt;

public final class ExamAttemptValidator {

    private ExamAttemptValidator() {}

    public static void validateAnswerAttempt(ExamAttempt attempt, int examId, int studentId) {
        if (attempt == null) {
            throw new AttemptAnswerException(404, "Exam attempt not found");
        }
        if (attempt.getExam() == null || attempt.getExam().getExamId() != examId) {
            throw new AttemptAnswerException(409, "Attempt does not belong to this exam");
        }
        if (attempt.getStudent() == null || attempt.getStudent().getStudentId() != studentId) {
            throw new AttemptAnswerException(403, "You are not authorized to modify this attempt");
        }
        validateAttemptCanAnswer(attempt);
    }

    public static void validateViewAttempt(ExamAttempt attempt, int examId, int studentId) {
        if (attempt == null) {
            throw new AttemptAnswerException(404, "Exam attempt not found");
        }
        if (attempt.getStudent() == null || attempt.getStudent().getStudentId() != studentId) {
            throw new AttemptAnswerException(403, "You are not authorized to view this attempt");
        }
        if (attempt.getExam() == null || attempt.getExam().getExamId() != examId) {
            throw new AttemptAnswerException(409, "Attempt does not belong to this exam");
        }
    }

    public static void validateAttemptCanAnswer(ExamAttempt attempt) {
        if (!ExamConstants.ATTEMPT_STATUS_IN_PROGRESS.equalsIgnoreCase(attempt.getStatus())) {
            if (ExamConstants.ATTEMPT_STATUS_SUBMITTED.equalsIgnoreCase(attempt.getStatus())) {
                throw new AttemptAnswerException(409, "Exam attempt has already been submitted");
            }
            throw new AttemptAnswerException(409, "Exam attempt cannot be modified");
        }
        if (attempt.getExpiresAt() == null || !LocalDateTime.now().isBefore(attempt.getExpiresAt())) {
            throw new AttemptAnswerException(409, "Exam attempt has expired");
        }
    }

    public static void validateSubmitAttempt(ExamAttempt attempt, int examId, int studentId) {
        if (attempt == null) {
            throw new ExamSubmissionException(404, "Exam attempt not found");
        }
        if (attempt.getStudent() == null || attempt.getStudent().getStudentId() != studentId) {
            throw new ExamSubmissionException(403, "You are not authorized to submit this attempt");
        }
        if (attempt.getExam() == null || attempt.getExam().getExamId() != examId) {
            throw new ExamSubmissionException(409, "Attempt does not belong to this exam");
        }
    }

    public static void validateAttemptCanSubmit(ExamAttempt attempt) {
        if (!ExamConstants.ATTEMPT_STATUS_IN_PROGRESS.equalsIgnoreCase(attempt.getStatus())) {
            throw new ExamSubmissionException(409, "Exam attempt cannot be submitted");
        }
    }
}
