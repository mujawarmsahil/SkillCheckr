package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;

public interface ResultService {

	ExamResultDTO submitExam(ExamSubmissionDTO submission);

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	ExamResultDTO getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();
}
