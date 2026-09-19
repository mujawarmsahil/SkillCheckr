package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.AttemptAnswer;

public interface AttemptAnswerRepository {

    List<AttemptAnswer> findByAttemptId(int attemptId);

    boolean answerBelongsToQuestion(int answerId, int questionId);

    AttemptAnswer saveAndReturn(AttemptAnswer answer);

    boolean updateMarks(int attemptId, int questionId, Integer marksObtained);
}
