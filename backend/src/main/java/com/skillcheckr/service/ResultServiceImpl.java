package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.repository.ResultRepository;

@Service
public class ResultServiceImpl implements ResultService {

	@Autowired
	private ResultRepository resultRepository;

	@Override
	public ExamResultDTO submitExam(ExamSubmissionDTO submission) {
		return resultRepository.submitExam(submission);
	}

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		return resultRepository.getResultsByStudentId(studentId);
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		return resultRepository.getAllResults();
	}
}
