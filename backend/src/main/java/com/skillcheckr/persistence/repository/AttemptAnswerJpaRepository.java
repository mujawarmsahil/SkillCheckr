package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.AttemptAnswerEntity;

public interface AttemptAnswerJpaRepository extends JpaRepository<AttemptAnswerEntity, Integer> {

    List<AttemptAnswerEntity> findByAttemptIdOrderByQuestionIdAsc(int attemptId);

    @Query(value = "SELECT COUNT(*) FROM answer WHERE answer_id = :answerId AND question_id = :questionId", nativeQuery = true)
    int countAnswerForQuestion(@Param("answerId") int answerId, @Param("questionId") int questionId);

    @Query(value = "SELECT COUNT(*) FROM attempt_answer WHERE question_id = :questionId", nativeQuery = true)
    int countHistoryForQuestion(@Param("questionId") int questionId);

    @Modifying
    @Query("UPDATE AttemptAnswerEntity a SET a.marksObtained = :marks WHERE a.attemptId = :attemptId AND a.questionId = :questionId")
    int updateMarks(@Param("attemptId") int attemptId, @Param("questionId") int questionId, @Param("marks") Integer marks);
}
