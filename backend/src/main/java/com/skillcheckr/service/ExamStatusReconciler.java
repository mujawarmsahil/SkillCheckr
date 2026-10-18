package com.skillcheckr.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.skillcheckr.repository.ExamRepository;

/** Persists time-derived completion status outside exam read requests. */
@Component
public class ExamStatusReconciler {

    private final ExamRepository examRepository;

    public ExamStatusReconciler(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Scheduled(initialDelayString = "${app.exam.status-reconciler-initial-delay-ms:300000}",
            fixedDelayString = "${app.exam.status-reconciler-delay-ms:300000}")
    public void reconcileCompletedExams() {
        examRepository.syncExamStatuses();
    }
}
