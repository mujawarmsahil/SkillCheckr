package com.skillcheckr.service;

import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.AttemptAnswerResponse;
import java.util.List;

public interface AttemptAnswerService {

    AttemptAnswerResponse saveAnswer(int examId, int attemptId, int questionId, int studentId,
            AttemptAnswerRequest request);

    List<AttemptAnswerResponse> getAnswers(int examId, int attemptId, int studentId);
}
