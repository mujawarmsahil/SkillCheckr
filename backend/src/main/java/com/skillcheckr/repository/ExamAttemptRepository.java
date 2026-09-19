package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamAttempt;

public interface ExamAttemptRepository {

    List<ExamAttempt> findByExamIdAndStudentId(int examId, int studentId);

    ExamAttempt findById(int attemptId);

    ExamAttempt findByIdForUpdate(int attemptId);

    boolean finalizeAttempt(int attemptId, java.time.LocalDateTime submittedAt);

    int createAttempt(ExamAttempt attempt);
}
