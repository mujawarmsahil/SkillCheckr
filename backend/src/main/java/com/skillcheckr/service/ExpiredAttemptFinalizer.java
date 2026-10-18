package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.repository.ExamAttemptRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExpiredAttemptFinalizer {

    private static final int BATCH_SIZE = 250;

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamSubmissionService examSubmissionService;
    private final AttemptExpiryPolicy attemptExpiryPolicy;

    @Autowired(required = false)
    private Clock clock;

    public ExpiredAttemptFinalizer(ExamAttemptRepository examAttemptRepository,
            ExamSubmissionService examSubmissionService, AttemptExpiryPolicy attemptExpiryPolicy) {
        this.examAttemptRepository = examAttemptRepository;
        this.examSubmissionService = examSubmissionService;
        this.attemptExpiryPolicy = attemptExpiryPolicy;
    }

    @Scheduled(initialDelayString = "${app.exam.attempt-finalizer-initial-delay-ms:0}",
            fixedDelayString = "${app.exam.attempt-finalizer-delay-ms:60000}")
    public void finalizeExpiredAttempts() {
        int afterAttemptId = 0;
        while (true) {
            List<ExamAttempt> attempts = examAttemptRepository.findInProgressAttemptsAfterId(afterAttemptId,
                    BATCH_SIZE);
            if (attempts.isEmpty()) {
                return;
            }
            for (ExamAttempt attempt : attempts) {
                afterAttemptId = attempt.getAttemptId();
                LocalDateTime now = clock == null ? LocalDateTime.now() : LocalDateTime.now(clock);
                if (!attemptExpiryPolicy.isExpired(attempt, attempt.getExam(), now)) {
                    continue;
                }
                try {
                    examSubmissionService.submit(attempt.getExam().getExamId(), attempt.getAttemptId(),
                            attempt.getStudent().getStudentId());
                } catch (RuntimeException ex) {
                    log.error("Failed to finalize expired exam attempt {}", attempt.getAttemptId(), ex);
                }
            }
            if (attempts.size() < BATCH_SIZE) {
                return;
            }
        }
    }
}
