package com.skillcheckr.repository;

import java.util.Optional;

import com.skillcheckr.model.QuestionBankDocument;

public interface QuestionBankRepository {

    void save(int examId, String fileName, byte[] content);

    Optional<QuestionBankDocument> findByExamId(int examId);
}
