package com.skillcheckr.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.ExamAttemptEntity;

public interface ExamAttemptJpaRepository extends JpaRepository<ExamAttemptEntity, Integer> {

    List<ExamAttemptEntity> findByExamIdAndStudentIdOrderByAttemptIdDesc(int examId, int studentId);

    @Query(value = "SELECT attempt_id, exam_id, student_id, started_at, expires_at, submitted_at, status "
            + "FROM exam_attempt WHERE attempt_id = ? FOR UPDATE", nativeQuery = true)
    List<ExamAttemptEntity> findByIdForUpdate(@Param("attemptId") int attemptId);

    @Query(value = "SELECT a.attempt_id, a.exam_id, a.student_id, a.started_at, a.expires_at, "
            + "a.submitted_at, a.status, e.exam_date, e.start_time, e.end_time "
            + "FROM exam_attempt a JOIN exam e ON e.exam_id = a.exam_id "
            + "WHERE a.status = :status AND a.attempt_id > :afterAttemptId ORDER BY a.attempt_id LIMIT :limit", nativeQuery = true)
    List<Object[]> findInProgressAttemptsAfterIdRaw(@Param("status") String status,
            @Param("afterAttemptId") int afterAttemptId, @Param("limit") int limit);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM exam_attempt WHERE exam_id = :examId)", nativeQuery = true)
    Boolean existsForExam(@Param("examId") int examId);

    @Modifying
    @Query("UPDATE ExamAttemptEntity a SET a.status = :submitted, a.submittedAt = :submittedAt "
            + "WHERE a.attemptId = :attemptId AND a.status = :inProgress")
    int finalizeAttempt(@Param("attemptId") int attemptId, @Param("submitted") String submitted,
            @Param("submittedAt") LocalDateTime submittedAt, @Param("inProgress") String inProgress);
    @Query(value = "SELECT COUNT(*) FROM exam_attempt WHERE student_id = :studentId", nativeQuery = true)
    int countByStudentId(@Param("studentId") int studentId);
}
