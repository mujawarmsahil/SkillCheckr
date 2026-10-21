package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.ExamEntity;

public interface ExamJpaRepository extends JpaRepository<ExamEntity, Integer> {

    List<ExamEntity> findBySubjectIdOrderByExamIdAsc(int subjectId);

    List<ExamEntity> findByTeacherIdOrderByExamIdDesc(int teacherId);

    @Modifying
    @Query("UPDATE ExamEntity e SET e.status = :status, e.paperFrozen = CASE WHEN LOWER(:status) IN ('upcoming','approved') THEN TRUE ELSE e.paperFrozen END WHERE e.examId = :examId")
    int updateStatus(@Param("examId") int examId, @Param("status") String status);

    @Modifying
    @Query("UPDATE ExamEntity e SET e.status = 'Upcoming', e.paperFrozen = TRUE WHERE e.examId = :examId")
    int acceptExam(@Param("examId") int examId);

    @Modifying
    @Query(value = "UPDATE exam SET status = 'Completed' WHERE (status = 'Upcoming' OR status = 'Approved') AND (DATE(exam_date) < CURDATE() OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME()))", nativeQuery = true)
    int syncExamStatuses();

    @Modifying
    @Query(value = "DELETE FROM exam_question WHERE exam_id = :examId", nativeQuery = true)
    int deleteExamQuestionLinks(@Param("examId") int examId);

    @Query(value = "SELECT exam_id FROM exam WHERE subject_id = :subjectId ORDER BY exam_id", nativeQuery = true)
    List<Integer> findExamIdsBySubjectId(@Param("subjectId") int subjectId);

    @Query(value = "SELECT COUNT(*) FROM exam WHERE exam_id = :examId", nativeQuery = true)
    int countExam(@Param("examId") int examId);

    @Query(value = "SELECT COUNT(*) FROM exam_attempt WHERE exam_id = :examId", nativeQuery = true)
    int countAttempts(@Param("examId") int examId);

    @Query(value = "SELECT COUNT(*) FROM exam_registration WHERE exam_id = :examId", nativeQuery = true)
    int countRegistrations(@Param("examId") int examId);

    @Query(value = "SELECT exam_id FROM exam WHERE exam_id = ? FOR UPDATE", nativeQuery = true)
    List<Integer> lockExamForUpdate(@Param("examId") int examId);

    @Query(value = "SELECT subject_id FROM exam WHERE exam_id = :examId", nativeQuery = true)
    List<Integer> findExamSubjectId(@Param("examId") int examId);
}
