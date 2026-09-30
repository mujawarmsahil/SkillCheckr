package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamQuestion;

public interface ExamQuestionRepository {

    List<ExamQuestion> findByExamId(int examId);

    boolean isQuestionAssigned(int examId, int questionId);

    boolean isQuestionAssignedToAnyExam(int questionId);

    int getNextQuestionOrder(int examId);

    boolean attachQuestion(int examId, int questionId, int questionOrder);

    List<Integer> findSubjectIdsByQuestionId(int questionId);
}
