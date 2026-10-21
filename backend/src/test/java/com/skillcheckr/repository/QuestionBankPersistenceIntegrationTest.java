package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.skillcheckr.model.QuestionBankDocument;

/**
 * Runs the repository against the H2 (MySQL-mode) schema generated for the test context,
 * covering insert, overwrite, and missing-document lookup on a real database rather than
 * a mock.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(QuestionBankRepositoryImpl.class)
class QuestionBankPersistenceIntegrationTest {

    @Autowired
    private QuestionBankRepository repository;

    @Test
    void saveInsertsTheDocumentAndFindReturnsIt() {
        byte[] pdf = "%PDF-1.7 test".getBytes(StandardCharsets.US_ASCII);

        repository.save(11, "chapter-1.pdf", pdf);

        QuestionBankDocument found = repository.findByExamId(11).orElseThrow();
        assertThat(found.fileName()).isEqualTo("chapter-1.pdf");
        assertThat(found.content()).isEqualTo(pdf);
    }

    @Test
    void saveOverwritesFileNameAndContentForTheSameExam() {
        repository.save(12, "first.pdf", "%PDF-1.7 first".getBytes(StandardCharsets.US_ASCII));

        repository.save(12, "second.pdf", "%PDF-1.7 second-content".getBytes(StandardCharsets.US_ASCII));

        QuestionBankDocument found = repository.findByExamId(12).orElseThrow();
        assertThat(found.fileName()).isEqualTo("second.pdf");
        assertThat(found.content()).isEqualTo("%PDF-1.7 second-content".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void findByExamIdIsEmptyWhenNoDocumentWasUploaded() {
        assertThat(repository.findByExamId(99)).isEmpty();
    }
}
