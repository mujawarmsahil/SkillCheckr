package com.skillcheckr.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.model.QuestionBankDocument;
import com.skillcheckr.persistence.entity.QuestionBankDocumentEntity;
import com.skillcheckr.persistence.repository.QuestionBankDocumentJpaRepository;

@Repository
public class QuestionBankRepositoryImpl implements QuestionBankRepository {

    private final QuestionBankDocumentJpaRepository documentRepository;

    public QuestionBankRepositoryImpl(QuestionBankDocumentJpaRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Upsert by primary key. Callers run inside a transaction after
     * {@code QuestionPaperMutationPolicy.lockAndValidate} has taken the exam row lock,
     * so concurrent uploads for the same exam are serialized and the read-then-write
     * below cannot interleave.
     */
    @Override
    @Transactional
    public void save(int examId, String fileName, byte[] content) {
        QuestionBankDocumentEntity entity = documentRepository.findById(examId)
                .orElseGet(QuestionBankDocumentEntity::new);
        entity.setExamId(examId);
        entity.setFileName(fileName);
        entity.setPdfContent(content);
        documentRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestionBankDocument> findByExamId(int examId) {
        return documentRepository.findById(examId)
                .map(entity -> new QuestionBankDocument(entity.getFileName(), entity.getPdfContent()));
    }
}
