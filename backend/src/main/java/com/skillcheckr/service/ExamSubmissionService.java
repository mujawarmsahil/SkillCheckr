package com.skillcheckr.service;

import com.skillcheckr.model.ExamResultDTO;

public interface ExamSubmissionService {

    ExamResultDTO submit(int examId, int attemptId, int studentId);
}
