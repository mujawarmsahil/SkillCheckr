package com.skillcheckr.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.service.QuestionService;

@RestController
@RequestMapping({"/api/create", "/api/questions"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class QuestionController {

	@Autowired
	private QuestionService questionService;

	@PostMapping({"/addQues", ""})
	public ResponseEntity<?> addAllQuestion(@RequestBody List<QuestionDTO> questions) {
		try {
			questionService.saveQuestionsWithAnswers(questions);
			Map<String, Object> response = new HashMap<>();
			response.put("message", "Questions added successfully");
			response.put("count", questions != null ? questions.size() : 0);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to add questions: " + e.getMessage()));
		}
	}

	@GetMapping("/subject/{subjectId}")
	public ResponseEntity<?> getQuestionsBySubject(@PathVariable("subjectId") Integer subjectId) {
		List<QuestionDTO> list = questionService.getQuestionsBySubjectId(subjectId);
		return ResponseEntity.ok(list != null ? list : List.of());
	}

	@GetMapping("/exam/{examId}")
	public ResponseEntity<?> getQuestionsByExam(@PathVariable("examId") Integer examId) {
		List<QuestionDTO> list = questionService.getQuestionsByExamId(examId);
		return ResponseEntity.ok(list != null ? list : List.of());
	}

	@DeleteMapping("/{questionId}")
	public ResponseEntity<?> deleteQuestion(@PathVariable("questionId") Integer questionId) {
		boolean deleted = questionService.deleteQuestionById(questionId);
		if (deleted) {
			return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Question not found"));
	}
}
