package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;

public interface ResultService {

	ExamResultDTO submitExam(ExamSubmissionDTO submission);

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();
}
