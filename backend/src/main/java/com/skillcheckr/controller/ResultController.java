package com.skillcheckr.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.model.SubmissionStatusResponse;
import com.skillcheckr.service.ResultService;

@RestController
@RequestMapping({"/api/results", "/api/Results"})
public class ResultController {

	@Autowired
	private ResultService resultService;

	@PostMapping({"/submit", ""})
	public ResponseEntity<ExamResultDTO> submitExam(@RequestBody ExamSubmissionDTO submission) {
		if (submission == null || submission.getExamId() <= 0) {
			throw new BadRequestException("Invalid submission data: exam ID is required");
		}

		// Prevent duplicate submission if already exists
		int studentId = submission.getStudentId();
		int examId = submission.getExamId();
		if (studentId > 0 && examId > 0) {
			Optional<ExamResultDTO> existing = resultService.getResultByExamAndStudent(examId, studentId);
			if (existing.isPresent()) {
				return ResponseEntity.ok(existing.get());
			}
		}

		ExamResultDTO result = resultService.submitExam(submission);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/check/{examId}/{studentId}")
	public ResponseEntity<SubmissionStatusResponse> checkStudentExamStatus(
			@PathVariable("examId") Integer examId,
			@PathVariable("studentId") Integer studentId) {
		Optional<ExamResultDTO> existing = resultService.getResultByExamAndStudent(examId, studentId);
		SubmissionStatusResponse response = existing
				.map(result -> SubmissionStatusResponse.builder().hasSubmitted(true).result(result).build())
				.orElseGet(() -> SubmissionStatusResponse.builder().hasSubmitted(false).build());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/student/{studentId}")
	public ResponseEntity<List<ExamResultDTO>> getResultsByStudent(@PathVariable("studentId") Integer studentId) {
		List<ExamResultDTO> results = resultService.getResultsByStudentId(studentId);
		return ResponseEntity.ok(results != null ? results : List.of());
	}

	@GetMapping({"/all", ""})
	public ResponseEntity<List<ExamResultDTO>> getAllResults() {
		List<ExamResultDTO> results = resultService.getAllResults();
		return ResponseEntity.ok(results != null ? results : List.of());
	}
}