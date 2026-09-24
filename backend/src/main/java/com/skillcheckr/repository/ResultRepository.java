package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamResultDTO;

public interface ResultRepository {

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	ExamResultDTO getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();

	ExamResultDTO findByAttemptId(int attemptId);

	ExamResultDTO insertSubmissionResult(ExamResultDTO result);

	int createSubmittedAttempt(int examId, int studentId);
}