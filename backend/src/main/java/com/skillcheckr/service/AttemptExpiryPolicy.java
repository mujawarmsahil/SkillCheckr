package com.skillcheckr.service;

import java.time.DateTimeException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.validation.ExamCreationValidator;

@Service
public class AttemptExpiryPolicy {

    public LocalDateTime effectiveExpiry(ExamAttempt attempt, Exam exam) {
        if (attempt == null || attempt.getExpiresAt() == null || exam == null) {
            return null;
        }
        try {
            LocalDateTime scheduledEnd = ExamCreationValidator.resolveWindowEnd(exam);
            return attempt.getExpiresAt().isBefore(scheduledEnd) ? attempt.getExpiresAt() : scheduledEnd;
        } catch (DateTimeException | NullPointerException ex) {
            return null;
        }
    }

    public boolean isExpired(ExamAttempt attempt, Exam exam, LocalDateTime now) {
        LocalDateTime expiry = effectiveExpiry(attempt, exam);
        return expiry == null || !now.isBefore(expiry);
    }
}
