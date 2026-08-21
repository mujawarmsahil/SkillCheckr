package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;

public interface ResultRepository {

	ExamResultDTO submitExam(ExamSubmissionDTO submission);

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	List<ExamResultDTO> getAllResults();
}
