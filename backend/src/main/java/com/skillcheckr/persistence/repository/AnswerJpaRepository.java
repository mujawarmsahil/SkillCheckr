package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.AnswerEntity;

public interface AnswerJpaRepository extends JpaRepository<AnswerEntity, Integer> {

    List<AnswerEntity> findByQuestionIdOrderByAnswerIdAsc(int questionId);

    @Modifying
    @Query("DELETE FROM AnswerEntity a WHERE a.questionId = :questionId")
    int deleteByQuestionId(@Param("questionId") int questionId);
}
