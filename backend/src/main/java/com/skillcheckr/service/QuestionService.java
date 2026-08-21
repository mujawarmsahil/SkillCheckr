package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.QuestionDTO;

public interface QuestionService {

	void saveQuestionsWithAnswers(List<QuestionDTO> questions);

	List<QuestionDTO> getQuestionsBySubjectId(int subjectId);

	List<QuestionDTO> getQuestionsByExamId(int examId);

	boolean deleteQuestionById(int questionId);
}
