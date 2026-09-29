package com.skillcheckr.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.QuestionsAddResponse;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.security.AuthPrincipal;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

	@Autowired
	private QuestionService questionService;

	@Autowired
	private ExamService examService;

	@PostMapping("")
	public ResponseEntity<QuestionsAddResponse> addAllQuestion(@RequestBody List<QuestionDTO> questions,
			HttpServletRequest request) {
		AuthPrincipal principal = AuthGuard.requireStaff(request);
		if (questions == null || questions.isEmpty()) {
			throw new BadRequestException("At least one question is required");
		}
		for (QuestionDTO question : questions) {
			if (question != null && question.getExamId() != null && question.getExamId() > 0) {
				// A teacher may only add questions to the exams they own.
				AuthGuard.requireExamAccess(request,
						examService.getExamById(question.getExamId())
								.orElseThrow(() -> new ResourceNotFoundException("Exam not found")));
			}
		}
		questionService.saveQuestionsWithAnswers(questions);
		return ResponseEntity.ok(QuestionsAddResponse.builder()
				.message("Questions added successfully")
				.count(questions.size())
				.build());
	}

	@GetMapping("")
	public ResponseEntity<List<QuestionDTO>> getAllQuestions(HttpServletRequest request) {
		AuthGuard.requireStaff(request);
		List<QuestionDTO> questions = questionService.getAllQuestions();
		return ResponseEntity.ok(questions != null ? questions : List.of());
	}

	@GetMapping("/{questionId}")
	public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable("questionId") Integer questionId,
			HttpServletRequest request) {
		AuthGuard.requireStaff(request);
		QuestionDTO q = questionService.getQuestionById(questionId)
				.orElseThrow(() -> new ResourceNotFoundException("Question not found"));
		return ResponseEntity.ok(q);
	}

	@PutMapping("/{questionId}")
	public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable("questionId") Integer questionId,
			@RequestBody QuestionDTO question, HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		if (question == null) {
			throw new BadRequestException("Invalid question payload");
		}
		question.setQuestionId(questionId);
		boolean updated = questionService.updateQuestion(question);
		if (!updated) {
			throw new ResourceNotFoundException("Question not found or update failed");
		}
		return ResponseEntity.ok(questionService.getQuestionById(questionId).orElse(question));
	}

	@GetMapping("/subject/{subjectId}")
	public ResponseEntity<List<QuestionDTO>> getQuestionsBySubject(@PathVariable("subjectId") Integer subjectId,
			HttpServletRequest request) {
		AuthGuard.requireStaff(request);
		List<QuestionDTO> questions = questionService.getQuestionsBySubjectId(subjectId);
		return ResponseEntity.ok(questions != null ? questions : List.of());
	}

	@GetMapping("/exam/{examId}")
	public ResponseEntity<List<QuestionDTO>> getQuestionsByExam(@PathVariable("examId") Integer examId,
			HttpServletRequest request) {
		AuthGuard.requireStaff(request);
		List<QuestionDTO> questions = questionService.getQuestionsByExamId(examId);
		return ResponseEntity.ok(questions != null ? questions : List.of());
	}

	@DeleteMapping("/{questionId}")
	public ResponseEntity<ApiResponse> deleteQuestion(@PathVariable("questionId") Integer questionId,
			HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		boolean deleted = questionService.deleteQuestionById(questionId);
		if (deleted) {
			return ResponseEntity.ok(new ApiResponse(true, "Question deleted successfully"));
		}
		return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Question not found"));
	}
}
