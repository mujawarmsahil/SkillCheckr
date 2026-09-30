package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.ExamResultDTO;

public interface ResultService {

	List<ExamResultDTO> getResultsByStudentId(int studentId);

	Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId);

	List<ExamResultDTO> getAllResults();
}
