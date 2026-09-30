package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;

public interface ResultRepository {

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();

	Optional<ExamResultDTO> findByAttemptId(int attemptId);

	ExamResultDTO insertSubmissionResult(ExamResultDTO result);

	List<ExamSubmissionSummaryDTO> findSubmissionSummariesByExamId(int examId);

	boolean updateResultAfterEvaluation(int attemptId, int marksObtained, int totalMarks, int passingMarks,
			double percentage, String status);
}
