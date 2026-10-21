package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.ExamQuestionEntity;

public interface ExamQuestionJpaRepository extends JpaRepository<ExamQuestionEntity, Integer> {

    List<ExamQuestionEntity> findByExamIdOrderByQuestionOrderAsc(int examId);

    @Modifying
    @Query("DELETE FROM ExamQuestionEntity e WHERE e.examId = :examId AND e.questionId = :questionId")
    int deleteByExamIdAndQuestionId(@Param("examId") int examId, @Param("questionId") int questionId);

    @Modifying
    @Query(value = "DELETE FROM exam_question WHERE exam_id = :examId", nativeQuery = true)
    int deleteByExamId(@Param("examId") int examId);

    @Query(value = "SELECT question_id FROM exam_question WHERE exam_id = :examId ORDER BY question_order ASC", nativeQuery = true)
    List<Integer> findQuestionIdsByExamId(@Param("examId") int examId);
}
