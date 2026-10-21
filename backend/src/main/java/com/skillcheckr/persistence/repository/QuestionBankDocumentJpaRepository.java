package com.skillcheckr.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.QuestionBankDocumentEntity;

/**
 * The exam id is the primary key of {@code exam_question_bank}, so the built-in
 * {@code findById}/{@code save} methods cover both the lookup and the upsert path.
 */
public interface QuestionBankDocumentJpaRepository extends JpaRepository<QuestionBankDocumentEntity, Integer> {
}
