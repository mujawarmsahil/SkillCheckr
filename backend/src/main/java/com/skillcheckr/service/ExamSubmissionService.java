package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.EvaluationItemDTO;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;

public interface ExamSubmissionService {

    ExamResultDTO submit(int examId, int attemptId, int studentId);

    List<ExamSubmissionSummaryDTO> listSubmissions(int examId);

    List<EvaluationItemDTO> getEvaluationItems(int examId, int attemptId);

    ExamResultDTO awardAnswerMarks(int examId, int attemptId, int questionId, int marks);
}
