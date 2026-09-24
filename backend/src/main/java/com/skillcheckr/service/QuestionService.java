package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.QuestionDTO;

public interface QuestionService {

	void saveQuestionsWithAnswers(List<QuestionDTO> questions);

	List<QuestionDTO> getAllQuestions();

	Optional<QuestionDTO> getQuestionById(int questionId);

	List<QuestionDTO> getQuestionsBySubjectId(int subjectId);

	List<QuestionDTO> getQuestionsByExamId(int examId);

	boolean updateQuestion(QuestionDTO question);

	boolean deleteQuestionById(int questionId);
}
