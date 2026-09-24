package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.ExamResultDTO;

public interface ResultRepository {

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();

	Optional<ExamResultDTO> findByAttemptId(int attemptId);

	ExamResultDTO insertSubmissionResult(ExamResultDTO result);

	int createSubmittedAttempt(int examId, int studentId);
}