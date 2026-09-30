package com.skillcheckr.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.ForbiddenException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.SubmissionStatusResponse;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.security.AuthPrincipal;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.ResultService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/results")
public class ResultController {

	@Autowired
	private ResultService resultService;

	@Autowired
	private ExamService examService;

	@GetMapping("/check/{examId}/{studentId}")
	public ResponseEntity<SubmissionStatusResponse> checkStudentExamStatus(
			@PathVariable("examId") Integer examId,
			@PathVariable("studentId") Integer studentId,
			HttpServletRequest request) {
		requireResultReadAccess(request, examId, studentId);
		Optional<ExamResultDTO> existing = resultService.getResultByExamAndStudent(examId, studentId);
		SubmissionStatusResponse response = existing
				.map(result -> SubmissionStatusResponse.builder().hasSubmitted(true).result(result).build())
				.orElseGet(() -> SubmissionStatusResponse.builder().hasSubmitted(false).build());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/student/{studentId}")
	public ResponseEntity<List<ExamResultDTO>> getResultsByStudent(@PathVariable("studentId") Integer studentId,
			HttpServletRequest request) {
		// The list spans every exam the student took, so only the student and an admin may read it.
		AuthGuard.requireSelfOrAdminForRoleData(request, studentId);
		List<ExamResultDTO> results = resultService.getResultsByStudentId(studentId);
		return ResponseEntity.ok(results != null ? results : List.of());
	}

	/**
	 * A single exam result is readable by the student, by an admin, and by the teacher who
	 * owns the exam. Any other teacher is refused because exams are not shared between staff.
	 */
	private void requireResultReadAccess(HttpServletRequest request, Integer examId, Integer studentId) {
		AuthPrincipal principal = AuthGuard.requirePrincipal(request);
		if (principal.isAdmin() || principal.getRoleId() == studentId) {
			return;
		}
		if (principal.isTeacher()) {
			Exam exam = examService.getExamById(examId)
					.orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
			AuthGuard.requireExamAccess(request, exam);
			return;
		}
		throw new ForbiddenException("You are not authorized to access another user's result.");
	}

	@GetMapping("/all")
	public ResponseEntity<List<ExamResultDTO>> getAllResults(HttpServletRequest request) {
		// A cross student result list carries no exam context, so only an admin can read it.
		// Teachers review the submissions of their own exams through the exam scoped endpoints.
		AuthGuard.requireAdmin(request);
		List<ExamResultDTO> results = resultService.getAllResults();
		return ResponseEntity.ok(results != null ? results : List.of());
	}
}
