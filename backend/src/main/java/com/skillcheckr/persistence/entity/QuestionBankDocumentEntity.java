package com.skillcheckr.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam_question_bank")
@Getter
@Setter
@NoArgsConstructor
public class QuestionBankDocumentEntity {

    @Id
    @Column(name = "exam_id")
    private Integer examId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Lob
    @Column(name = "pdf_content", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] pdfContent;

    // uploaded_at is intentionally unmapped: the schema manages it with
    // DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP.
}
