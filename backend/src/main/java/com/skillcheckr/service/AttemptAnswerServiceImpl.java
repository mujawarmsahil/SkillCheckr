package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.AttemptAnswerResponse;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.exception.AttemptAnswerException;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.QuestionRepository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.validation.ExamAttemptValidator;

@Service
public class AttemptAnswerServiceImpl implements AttemptAnswerService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public AttemptAnswerServiceImpl(ExamAttemptRepository examAttemptRepository,
            ExamQuestionRepository examQuestionRepository, QuestionRepository questionRepository,
            AttemptAnswerRepository attemptAnswerRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    @Override
    @Transactional
    public AttemptAnswerResponse saveAnswer(int examId, int attemptId, int questionId, int studentId,
            AttemptAnswerRequest request) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId);
        ExamAttemptValidator.validateAnswerAttempt(attempt, examId, studentId);

        Question question = questionRepository.findQuestionDetailsById(questionId)
                .orElseThrow(() -> new AttemptAnswerException(404, "Question not found"));
        if (!examQuestionRepository.isQuestionAssigned(examId, questionId)) {
            throw new AttemptAnswerException(409, "Question does not belong to this exam");
        }

        Integer selectedAnswerId = request == null ? null : request.getSelectedAnswerId();
        String textAnswer = request == null ? null : request.getTextAnswer();
        boolean hasText = textAnswer != null && !textAnswer.trim().isEmpty();
        boolean hasSelectedAnswer = selectedAnswerId != null;
        int selectedAnswerValue = selectedAnswerId == null ? 0 : selectedAnswerId;
        if (hasText && hasSelectedAnswer) {
            throw new AttemptAnswerException(400,
                    "Only the answer field applicable to the question type may be provided");
        }

        String questionType = question.getQuestionType();
        if (ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(questionType)) {
            if (hasText) {
                throw new AttemptAnswerException(400, "textAnswer is not valid for MCQ questions");
            }
            if (hasSelectedAnswer && !attemptAnswerRepository.answerBelongsToQuestion(selectedAnswerValue, questionId)) {
                throw new AttemptAnswerException(400, "Selected answer does not belong to this question");
            }
            textAnswer = null;
        } else if (ExamConstants.QUESTION_TYPE_QUESTION_ANSWER.equalsIgnoreCase(questionType)) {
            if (hasSelectedAnswer) {
                throw new AttemptAnswerException(400, "selectedAnswerId is not valid for text questions");
            }
            if (!hasText) {
                textAnswer = null;
            } else {
                textAnswer = textAnswer == null ? null : textAnswer.trim();
                if (question.getWordLimit() != null && countWords(textAnswer) > question.getWordLimit()) {
                    throw new AttemptAnswerException(400, "Text answer exceeds the allowed word limit");
                }
            }
        } else {
            throw new AttemptAnswerException(400, "Unsupported question type");
        }

        AttemptAnswer saved = attemptAnswerRepository.saveAndReturn(AttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .selectedAnswer(selectedAnswerId == null ? null : Answer.builder().answerId(selectedAnswerId).build())
                .textAnswer(textAnswer)
                .marksObtained(null)
                .build());
        return AttemptAnswerResponse.builder()
                .attemptAnswerId(saved.getAttemptAnswerId())
                .attemptId(attemptId)
                .questionId(questionId)
                .selectedAnswerId(selectedAnswerId)
                .textAnswer(textAnswer)
                .build();
    }

    @Override
    public List<AttemptAnswerResponse> getAnswers(int examId, int attemptId, int studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId);
        ExamAttemptValidator.validateViewAttempt(attempt, examId, studentId);

        List<AttemptAnswerResponse> responses = new ArrayList<>();
        for (AttemptAnswer answer : attemptAnswerRepository.findByAttemptId(attemptId)) {
            int questionId = answer.getQuestion() == null ? 0 : answer.getQuestion().getQuestionId();
            responses.add(AttemptAnswerResponse.builder()
                    .attemptAnswerId(answer.getAttemptAnswerId())
                    .attemptId(attemptId)
                    .questionId(questionId)
                    .selectedAnswerId(answer.getSelectedAnswer() == null
                            ? null : answer.getSelectedAnswer().getAnswerId())
                    .textAnswer(answer.getTextAnswer())
                    .build());
        }
        return responses;
    }

    private int countWords(String value) {
        return value.trim().split("\\s+").length;
    }
}
