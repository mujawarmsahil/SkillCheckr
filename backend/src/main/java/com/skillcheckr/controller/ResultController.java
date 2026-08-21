package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.service.ResultService;

@RestController
@RequestMapping({"/api/results", "/api/Results"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class ResultController {

	@Autowired
	private ResultService resultService;

	@PostMapping({"/submit", ""})
	public ResponseEntity<?> submitExam(@RequestBody ExamSubmissionDTO submission) {
		try {
			if (submission == null || submission.getExamId() <= 0) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Map.of("message", "Invalid submission data: exam ID is required"));
			}

			ExamResultDTO result = resultService.submitExam(submission);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Error submitting exam: " + e.getMessage()));
		}
	}

	@GetMapping("/student/{studentId}")
	public ResponseEntity<?> getResultsByStudent(@PathVariable("studentId") Integer studentId) {
		List<ExamResultDTO> results = resultService.getResultsByStudentId(studentId);
		return ResponseEntity.ok(results != null ? results : List.of());
	}

	@GetMapping({"/all", ""})
	public ResponseEntity<?> getAllResults() {
		List<ExamResultDTO> results = resultService.getAllResults();
		return ResponseEntity.ok(results != null ? results : List.of());
	}
}
