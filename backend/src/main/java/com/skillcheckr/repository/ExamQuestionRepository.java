package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamQuestion;

public interface ExamQuestionRepository {

    List<ExamQuestion> findByExamId(int examId);

    boolean isQuestionAssigned(int examId, int questionId);

}
