package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.ExamQuestion;

public interface ExamQuestionRepository {

    List<ExamQuestion> findByExamId(int examId);

    boolean isQuestionAssigned(int examId, int questionId);

    boolean isQuestionAssignedToAnyExam(int questionId);

    int getNextQuestionOrder(int examId);

    /**
     * Attaches a question to an exam if it is not attached already.
     *
     * @return true only when a new exam_question row was inserted; false when the question
     *         was already attached or the insert affected no row
     */
    boolean attachQuestion(int examId, int questionId, int questionOrder);

    List<Integer> findSubjectIdsByQuestionId(int questionId);
}
