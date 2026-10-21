package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.SubjectEntity;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, Integer> {

    List<SubjectEntity> findAllByOrderBySubjectIdDesc();

    boolean existsBySubjectCode(String subjectCode);

    // Cross-table aggregates and the multi-table delete cascade stay as SQL: the
    // question/exam/answer/exam_question tables have no entities yet, and the
    // delete order encodes the FK-safe cascade the previous JDBC code performed.

    @Query(value = "SELECT COUNT(*) FROM question WHERE subject_id = :subjectId", nativeQuery = true)
    int countQuestionsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT COUNT(*) FROM exam WHERE subject_id = :subjectId", nativeQuery = true)
    int countExamsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT exam_id FROM exam WHERE subject_id = :subjectId", nativeQuery = true)
    List<Integer> findExamIdsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT question_id FROM question WHERE subject_id = :subjectId", nativeQuery = true)
    List<Integer> findQuestionIdsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT COUNT(*) FROM exam_attempt WHERE exam_id = :examId", nativeQuery = true)
    int countAttemptsByExamId(@Param("examId") int examId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM exam_question WHERE exam_id = :examId", nativeQuery = true)
    int deleteExamQuestionsByExamId(@Param("examId") int examId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM answer WHERE question_id = :questionId", nativeQuery = true)
    int deleteAnswersByQuestionId(@Param("questionId") int questionId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM question WHERE subject_id = :subjectId", nativeQuery = true)
    int deleteQuestionsBySubjectId(@Param("subjectId") int subjectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM exam WHERE subject_id = :subjectId", nativeQuery = true)
    int deleteExamsBySubjectId(@Param("subjectId") int subjectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM subject WHERE subject_id = :subjectId", nativeQuery = true)
    int deleteSubjectBySubjectId(@Param("subjectId") int subjectId);
}
