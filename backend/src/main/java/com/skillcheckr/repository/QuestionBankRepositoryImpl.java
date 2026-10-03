package com.skillcheckr.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.QuestionBankDocument;

@Repository
public class QuestionBankRepositoryImpl implements QuestionBankRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(int examId, String fileName, byte[] content) {
        String sql = "INSERT INTO exam_question_bank (exam_id, file_name, pdf_content) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), pdf_content = VALUES(pdf_content)";
        jdbcTemplate.update(sql, examId, fileName, content);
    }

    @Override
    public java.util.Optional<QuestionBankDocument> findByExamId(int examId) {
        String sql = "SELECT file_name, pdf_content FROM exam_question_bank WHERE exam_id = ?";
        List<QuestionBankDocument> documents = jdbcTemplate.query(sql,
                (rs, rowNum) -> new QuestionBankDocument(rs.getString("file_name"), rs.getBytes("pdf_content")),
                examId);
        return documents.stream().findFirst();
    }
}
