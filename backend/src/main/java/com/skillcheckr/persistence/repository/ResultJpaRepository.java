package com.skillcheckr.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.ResultEntity;

public interface ResultJpaRepository extends JpaRepository<ResultEntity, Integer> {

    Optional<ResultEntity> findByAttemptId(int attemptId);

    List<ResultEntity> findByStudentIdOrderByResultIdDesc(int studentId);

    @Modifying
    @Query("UPDATE ResultEntity r SET r.marksObtained = :marksObtained, r.totalMarks = :totalMarks, r.status = :status WHERE r.attemptId = :attemptId")
    int updateAfterEvaluation(@Param("attemptId") int attemptId, @Param("marksObtained") int marksObtained,
            @Param("totalMarks") int totalMarks, @Param("status") String status);
}
