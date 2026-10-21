package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.ExamRegistrationEntity;

public interface ExamRegistrationJpaRepository extends JpaRepository<ExamRegistrationEntity, Integer> {

    boolean existsByStudentIdAndExamId(int studentId, int examId);

    List<ExamRegistrationEntity> findByStudentIdOrderByRegistrationIdDesc(int studentId);

    @Modifying
    @Query(value = "INSERT INTO exam_registration (student_id, exam_id, status) VALUES (:studentId, :examId, 'Registered') ON DUPLICATE KEY UPDATE status = 'Registered'", nativeQuery = true)
    int upsertRegistration(@Param("studentId") int studentId, @Param("examId") int examId);

    @Modifying
    @Query("DELETE FROM ExamRegistrationEntity r WHERE r.studentId = :studentId AND r.examId = :examId")
    int deleteByStudentIdAndExamId(@Param("studentId") int studentId, @Param("examId") int examId);

    @Query(value = "SELECT COUNT(*) FROM exam_registration WHERE exam_id = :examId", nativeQuery = true)
    int countByExamId(@Param("examId") int examId);
}
