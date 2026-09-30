package com.skillcheckr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamQuestionRepository;

/**
 * Builds the per question review of a submitted attempt. The breakdown is always derived
 * from persisted answers, never from anything the browser sends, so the review a student
 * or teacher sees matches the stored attempt.
 */
@Service
public class QuestionBreakdownService {

	@Autowired
	private ExamQuestionRepository examQuestionRepository;

	@Autowired
	private AttemptAnswerRepository attemptAnswerRepository;

	public List<Map<String, Object>> build(int examId, int attemptId) {
		if (examId <= 0 || attemptId <= 0) {
			return List.of();
		}

		List<ExamQuestion> assignedQuestions = examQuestionRepository.findByExamId(examId);
		List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);

		Map<Integer, AttemptAnswer> answersByQuestion = new HashMap<>();
		for (AttemptAnswer answer : answers) {
			if (answer.getQuestion() != null) {
				answersByQuestion.put(answer.getQuestion().getQuestionId(), answer);
			}
		}

		List<Map<String, Object>> breakdown = new ArrayList<>();
		for (ExamQuestion assignment : assignedQuestions) {
			Question question = assignment.getQuestion();
			if (question == null) {
				continue;
			}
			AttemptAnswer answer = answersByQuestion.get(question.getQuestionId());
			boolean isMcq = ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(question.getQuestionType());
			boolean isCorrect = isMcq && answer != null && answer.getSelectedAnswer() != null
					&& answer.getSelectedAnswer().isCorrect();

			Map<String, Object> item = new HashMap<>();
			item.put("questionId", question.getQuestionId());
			item.put("question", question.getQuestionText());
			item.put("questionType", question.getQuestionType());
			item.put("marks", question.getMarks());
			item.put("awardedMarks", answer == null || answer.getMarksObtained() == null
					? 0
					: answer.getMarksObtained());
			item.put("evaluated", answer != null && answer.getMarksObtained() != null);
			item.put("requiresEvaluation", !isMcq);

			if (isMcq) {
				item.put("selectedAnswer", answer == null || answer.getSelectedAnswer() == null
						? null
						: answer.getSelectedAnswer().getOptionText());
				item.put("correctAnswer", answer == null ? null : answer.getCorrectOptionText());
				item.put("isCorrect", answer != null && answer.getSelectedAnswer() != null && isCorrect);
			} else {
				item.put("textAnswer", answer == null ? null : answer.getTextAnswer());
				item.put("isCorrect", null);
			}
			breakdown.add(item);
		}
		return breakdown;
	}
}
