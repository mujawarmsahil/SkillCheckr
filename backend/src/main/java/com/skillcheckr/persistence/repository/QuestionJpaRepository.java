package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.QuestionEntity;

public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, Integer> {

    List<QuestionEntity> findBySubjectIdOrderByQuestionIdAsc(int subjectId);

    @Modifying
    @Query(value = "DELETE FROM exam_question WHERE question_id = :questionId", nativeQuery = true)
    int deleteExamQuestionLinksByQuestionId(@Param("questionId") int questionId);

    @Query(value = "SELECT question_id FROM question WHERE subject_id = :subjectId ORDER BY question_id", nativeQuery = true)
    List<Integer> findQuestionIdsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT subject_id FROM exam WHERE exam_id = :examId", nativeQuery = true)
    List<Integer> findExamSubjectId(@Param("examId") int examId);
}
