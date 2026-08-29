package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.QuestionRepository;

@Service
public class QuestionServiceImpl implements QuestionService {

	private final QuestionRepository questionRepository;

	@Autowired
	public QuestionServiceImpl(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Override
	public void saveQuestionsWithAnswers(List<QuestionDTO> questions) {
		questionRepository.saveQuestionWithAnswers(questions);
	}

	@Override
	public List<QuestionDTO> getAllQuestions() {
		return questionRepository.getAllQuestions();
	}

	@Override
	public QuestionDTO getQuestionById(int questionId) {
		return questionRepository.getQuestionById(questionId);
	}

	@Override
	public List<QuestionDTO> getQuestionsBySubjectId(int subjectId) {
		return questionRepository.getQuestionsBySubjectId(subjectId);
	}

	@Override
	public List<QuestionDTO> getQuestionsByExamId(int examId) {
		return questionRepository.getQuestionsByExamId(examId);
	}

	@Override
	public boolean updateQuestion(QuestionDTO question) {
		return questionRepository.updateQuestion(question);
	}

	@Override
	public boolean deleteQuestionById(int questionId) {
		return questionRepository.deleteQuestionById(questionId);
	}
}