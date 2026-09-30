package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.repository.ResultRepository;

@Service
public class ResultServiceImpl implements ResultService {

	@Autowired
	private ResultRepository resultRepository;

	@Autowired
	private QuestionBreakdownService questionBreakdownService;

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		return withBreakdown(resultRepository.getResultsByStudentId(studentId));
	}

	@Override
	public Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId) {
		return resultRepository.getResultByExamAndStudent(examId, studentId)
				.map(result -> withBreakdown(result));
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		return withBreakdown(resultRepository.getAllResults());
	}

	private ExamResultDTO withBreakdown(ExamResultDTO result) {
		if (result != null && result.getAttemptId() > 0) {
			result.setQuestionBreakdown(questionBreakdownService.build(result.getExamId(), result.getAttemptId()));
		}
		return result;
	}

	private List<ExamResultDTO> withBreakdown(List<ExamResultDTO> results) {
		if (results == null) {
			return List.of();
		}
		results.forEach(this::withBreakdown);
		return results;
	}
}
