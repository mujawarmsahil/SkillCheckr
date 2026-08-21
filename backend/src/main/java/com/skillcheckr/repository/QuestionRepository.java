package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.QuestionDTO;

public interface QuestionRepository {

	void saveQuestionWithAnswers(List<QuestionDTO> questions);

	List<QuestionDTO> getQuestionsBySubjectId(int subjectId);

	List<QuestionDTO> getQuestionsByExamId(int examId);

	boolean deleteQuestionById(int questionId);
}
