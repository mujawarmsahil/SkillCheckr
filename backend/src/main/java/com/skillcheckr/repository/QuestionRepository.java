package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Question;

public interface QuestionRepository {

	void saveQuestionWithAnswers(List<QuestionDTO> questions);

	List<QuestionDTO> getAllQuestions();

	Optional<QuestionDTO> getQuestionById(int questionId);

	Optional<Question> findQuestionDetailsById(int questionId);

	List<QuestionDTO> getQuestionsBySubjectId(int subjectId);

	List<QuestionDTO> getQuestionsByExamId(int examId);

	boolean updateQuestion(QuestionDTO question);

	boolean deleteQuestionById(int questionId);
}

