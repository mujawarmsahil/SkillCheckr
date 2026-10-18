package com.skillcheckr.repository;

import java.util.List;
import java.util.Map;

import com.skillcheckr.model.AttemptAnswer;

public interface AttemptAnswerRepository {

    List<AttemptAnswer> findByAttemptId(int attemptId);

    List<AttemptAnswer> findByAttemptIds(List<Integer> attemptIds);

    boolean answerBelongsToQuestion(int answerId, int questionId);

    AttemptAnswer saveAndReturn(AttemptAnswer answer);

    boolean updateMarks(int attemptId, int questionId, Integer marksObtained);

    int updateMarksBatch(int attemptId, Map<Integer, Integer> marksByQuestionId);

    boolean hasAttemptHistory(int questionId);
}
