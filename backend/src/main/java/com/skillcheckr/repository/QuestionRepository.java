package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.QuestionDTO;

public interface QuestionRepository {

	void saveQuestionWithAnswers(List<QuestionDTO> questions);

	List<QuestionDTO> getAllQuestions();

	QuestionDTO getQuestionById(int questionId);

	List<QuestionDTO> getQuestionsBySubjectId(int subjectId);

	List<QuestionDTO> getQuestionsByExamId(int examId);

	boolean updateQuestion(QuestionDTO question);

	boolean deleteQuestionById(int questionId);
}

