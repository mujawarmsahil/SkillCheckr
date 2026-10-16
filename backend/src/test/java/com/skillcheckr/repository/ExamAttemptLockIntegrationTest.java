package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@JdbcTest
@Import(ExamAttemptRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExamAttemptLockIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExamAttemptRepositoryImpl repository;

    @BeforeEach
    void createAttemptTable() {
        jdbcTemplate.execute("SET NON_KEYWORDS EXAM");
        jdbcTemplate.execute("DROP TABLE IF EXISTS attempt_answer");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_attempt");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam");
        jdbcTemplate.execute("CREATE TABLE exam (exam_id INT PRIMARY KEY, exam_date DATETIME NOT NULL, "
                + "start_time TIME NOT NULL, end_time TIME NOT NULL)");
        jdbcTemplate.update("INSERT INTO exam VALUES (10, ?, '09:00:00', '10:00:00')",
                LocalDate.now().atStartOfDay());
        jdbcTemplate.execute("CREATE TABLE exam_attempt (attempt_id INT PRIMARY KEY, exam_id INT NOT NULL, "
                + "student_id INT NOT NULL, started_at TIMESTAMP NOT NULL, expires_at TIMESTAMP NOT NULL, "
                + "submitted_at TIMESTAMP NULL, status VARCHAR(32) NOT NULL)");
        jdbcTemplate.update("INSERT INTO exam_attempt VALUES (1, 10, 20, ?, ?, NULL, 'IN_PROGRESS')",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(30));
        jdbcTemplate.execute("CREATE TABLE attempt_answer (attempt_id INT PRIMARY KEY, answer_text VARCHAR(64))");
        jdbcTemplate.update("INSERT INTO attempt_answer VALUES (1, 'original')");
    }

    @Test
    void expiredAttemptScanIncludesTheScheduledWindowForEffectiveExpiry() {
        var attempts = repository.findInProgressAttemptsAfterId(0, 250);

        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getExam().getExamId()).isEqualTo(10);
        assertThat(attempts.get(0).getExam().getDate()).startsWith(LocalDate.now().toString());
        assertThat(attempts.get(0).getExam().getStartTime().toString()).isEqualTo("09:00");
        assertThat(attempts.get(0).getExam().getEndTime().toString()).isEqualTo("10:00");
    }

    @Test
    void concurrentAnswerSaveAndSubmissionAreSerialized() throws Exception {
        assertLockedReadsSerialize(false, false, "IN_PROGRESS", "first");
    }

    @Test
    void answerSaveAfterSubmissionRevalidatesTheCommittedAttemptStatus() throws Exception {
        assertLockedReadsSerialize(true, false, "SUBMITTED", "first");
    }

    @Test
    void concurrentAnswerSavesAreSerializedByTheAttemptLock() throws Exception {
        assertLockedReadsSerialize(false, true, "IN_PROGRESS", "second");
    }

    private void assertLockedReadsSerialize(boolean finalizeFirstTransaction, boolean secondIsAnswerSave,
            String expectedSecondStatus, String expectedAnswer) throws Exception {
        CountDownLatch firstTransactionHasLock = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondTransactionHasLock = new CountDownLatch(1);
        AtomicBoolean waitInterrupted = new AtomicBoolean();
        AtomicReference<String> secondAttemptStatus = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        TransactionTemplate firstTransaction = new TransactionTemplate(transactionManager);
        firstTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionTemplate secondTransaction = new TransactionTemplate(transactionManager);
        secondTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            Future<?> save = executor.submit(() -> firstTransaction.executeWithoutResult(status -> {
                repository.findByIdForUpdate(1);
                jdbcTemplate.update("UPDATE attempt_answer SET answer_text = 'first' WHERE attempt_id = 1");
                if (finalizeFirstTransaction) {
                    jdbcTemplate.update("UPDATE exam_attempt SET status = 'SUBMITTED' WHERE attempt_id = 1");
                }
                firstTransactionHasLock.countDown();
                try {
                    if (!releaseFirstTransaction.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release attempt lock");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    waitInterrupted.set(true);
                }
            }));
            assertThat(firstTransactionHasLock.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> submit = executor.submit(() -> secondTransaction.executeWithoutResult(status -> {
                secondAttemptStatus.set(repository.findByIdForUpdate(1).getStatus());
                if (secondIsAnswerSave) {
                    jdbcTemplate.update("UPDATE attempt_answer SET answer_text = 'second' WHERE attempt_id = 1");
                }
                secondTransactionHasLock.countDown();
            }));
            assertThat(secondTransactionHasLock.await(200, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirstTransaction.countDown();
            save.get(2, TimeUnit.SECONDS);
            submit.get(2, TimeUnit.SECONDS);

            assertThat(secondTransactionHasLock.getCount()).isZero();
            assertThat(secondAttemptStatus.get()).isEqualTo(expectedSecondStatus);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT answer_text FROM attempt_answer WHERE attempt_id = 1", String.class))
                    .isEqualTo(expectedAnswer);
            assertThat(waitInterrupted).isFalse();
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }
}
