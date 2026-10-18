package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptAnswerException;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.repository.ExamAttemptRepositoryImpl;
import com.skillcheckr.repository.ExamRepositoryImpl;

/**
 * Runs only through the mysql-integration Maven profile against a dedicated InnoDB schema.
 * Required environment variables are documented in backend/src/test/resources/application-mysql-it.properties.
 */
@SpringBootTest(properties = "app.exam.attempt-finalizer-initial-delay-ms=3600000")
@ActiveProfiles("mysql-it")
class AttemptTransactionsMySqlIT {

    private static final Pattern SAFE_TEST_DATABASE_URL = Pattern.compile(
            "^jdbc:mysql://[^/?#]+/(skillcheckr_it_[a-zA-Z0-9_]+)(?:\\?.*)?$");
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private AttemptAnswerService attemptAnswerService;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private ExamSubmissionService examSubmissionService;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AttemptExpiryPolicy attemptExpiryPolicy;

    @SpyBean
    private ExamAttemptRepositoryImpl examAttemptRepository;

    @SpyBean
    private ExamRepositoryImpl examRepository;

    private final AtomicReference<LockGate> activeGate = new AtomicReference<>();
    private final AtomicReference<ExamLockGate> activeExamGate = new AtomicReference<>();
    private int subjectId;
    private int studentId;
    private int examId;
    private int questionId;
    private int correctAnswerId;
    private int incorrectAnswerId;
    private int attemptId;
    private int paperExamId;
    private int paperQuestionId;
    private int extraPaperQuestionId;
    private int paperAttemptId;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        String url = requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_URL");
        Matcher matcher = SAFE_TEST_DATABASE_URL.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException("SKILLCHECKR_MYSQL_IT_URL must point to a dedicated "
                    + "MySQL database named skillcheckr_it_<suffix>; refusing to start integration tests");
        }
        String username = requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_USERNAME");
        String password = requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_PASSWORD");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.flyway.clean-disabled", () -> "true");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @BeforeEach
    void createFixtureAndVerifyDatabase() {
        assertThat(flyway.info().current()).as("Flyway must apply the actual application migrations").isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name IN "
                        + "('exam_attempt', 'attempt_answer', 'result') AND engine = 'InnoDB'",
                Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class))
                .isEqualToIgnoringCase("REPEATABLE-READ");

        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO,
                "MySQL {0}, database {1}, transaction isolation {2}",
                jdbcTemplate.queryForObject("SELECT VERSION()", String.class),
                jdbcTemplate.queryForObject("SELECT DATABASE()", String.class),
                jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class));

        subjectId = insertAndGetId("INSERT INTO subject (subject_name, subject_code) VALUES (?, ?)",
                "Concurrency IT", "IT-" + System.nanoTime());
        studentId = insertAndGetId("INSERT INTO student (name, email) VALUES (?, ?)",
                "Concurrency Test Student", "it-" + System.nanoTime() + "@example.test");
        examId = insertAndGetId("INSERT INTO exam "
                        + "(subject_id, exam_name, exam_type, exam_date, start_time, end_time, duration_minutes, "
                        + "total_marks, pass_marks, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                subjectId, "Concurrency Test Exam", ExamConstants.QUESTION_TYPE_MCQ,
                LocalDate.now().atStartOfDay(),
                LocalTime.now().minusHours(1), LocalTime.now().plusHours(4), 180, 5, 3,
                ExamConstants.EXAM_STATUS_APPROVED);
        questionId = insertAndGetId("INSERT INTO question "
                        + "(subject_id, question_text, question_type, marks) VALUES (?, ?, ?, ?)",
                subjectId, "Choose the correct option", ExamConstants.QUESTION_TYPE_MCQ, 5);
        correctAnswerId = insertAndGetId("INSERT INTO answer (question_id, option_text, is_correct) "
                        + "VALUES (?, ?, ?)",
                questionId, "Correct", true);
        incorrectAnswerId = insertAndGetId("INSERT INTO answer (question_id, option_text, is_correct) "
                        + "VALUES (?, ?, ?)",
                questionId, "Incorrect", false);
        jdbcTemplate.update("INSERT INTO exam_question (exam_id, question_id, question_order) VALUES (?, ?, 1)",
                examId, questionId);
        attemptId = insertAndGetId("INSERT INTO exam_attempt "
                        + "(exam_id, student_id, started_at, expires_at, status) VALUES (?, ?, ?, ?, ?)",
                examId, studentId, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2), ExamConstants.ATTEMPT_STATUS_IN_PROGRESS);
        jdbcTemplate.update("INSERT INTO attempt_answer "
                        + "(attempt_id, question_id, selected_answer_id, marks_obtained) VALUES (?, ?, ?, NULL)",
                attemptId, questionId, incorrectAnswerId);

        installLockGate();
        installExamLockGate();
    }

    @Test
    void coreFlowIndexesAreAppliedByFlyway() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND index_name IN "
                        + "('idx_attempt_status_id', 'idx_exam_question_order')",
                String.class)).contains("idx_attempt_status_id", "idx_exam_question_order");
    }

    @Test
    void answerUpsertReturnsTheExistingIdAndUpdatesNullableFields() {
        int initialAnswerId = jdbcTemplate.queryForObject(
                "SELECT attempt_answer_id FROM attempt_answer WHERE attempt_id = ? AND question_id = ?",
                Integer.class, attemptId, questionId);
        AttemptAnswer updatedMcq = AttemptAnswer.builder()
                .attempt(ExamAttempt.builder().attemptId(attemptId).build())
                .question(com.skillcheckr.model.Question.builder().questionId(questionId).build())
                .selectedAnswer(Answer.builder().answerId(correctAnswerId).build())
                .build();

        AttemptAnswer savedMcq = attemptAnswerRepository.saveAndReturn(updatedMcq);
        assertThat(savedMcq.getAttemptAnswerId()).isEqualTo(initialAnswerId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT selected_answer_id FROM attempt_answer WHERE attempt_id = ? AND question_id = ?",
                Integer.class, attemptId, questionId)).isEqualTo(correctAnswerId);

        AttemptAnswer updatedText = AttemptAnswer.builder()
                .attempt(ExamAttempt.builder().attemptId(attemptId).build())
                .question(Question.builder().questionId(questionId).build())
                .textAnswer("Updated draft")
                .build();
        AttemptAnswer savedText = attemptAnswerRepository.saveAndReturn(updatedText);

        assertThat(savedText.getAttemptAnswerId()).isEqualTo(initialAnswerId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT selected_answer_id FROM attempt_answer WHERE attempt_id = ? AND question_id = ?",
                Integer.class, attemptId, questionId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT text_answer FROM attempt_answer WHERE attempt_id = ? AND question_id = ?",
                String.class, attemptId, questionId)).isEqualTo("Updated draft");
    }

    @AfterEach
    void cleanFixtureAndResetSpies() {
        LockGate gate = activeGate.getAndSet(null);
        if (gate != null) {
            gate.release();
        }
        ExamLockGate examGate = activeExamGate.getAndSet(null);
        if (examGate != null) {
            examGate.release();
        }
        reset(examAttemptRepository);
        reset(examRepository);
        if (attemptId > 0) {
            jdbcTemplate.update("DELETE FROM result WHERE attempt_id = ?", attemptId);
            jdbcTemplate.update("DELETE FROM attempt_answer WHERE attempt_id = ?", attemptId);
            jdbcTemplate.update("DELETE FROM exam_attempt WHERE attempt_id = ?", attemptId);
        }
        if (examId > 0) {
            jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id = ?", examId);
            jdbcTemplate.update("DELETE FROM exam WHERE exam_id = ?", examId);
        }
        if (paperExamId > 0) {
            jdbcTemplate.update("DELETE FROM exam_attempt WHERE exam_id = ?", paperExamId);
            jdbcTemplate.update("DELETE FROM exam_registration WHERE exam_id = ?", paperExamId);
            jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id = ?", paperExamId);
            jdbcTemplate.update("DELETE FROM exam WHERE exam_id = ?", paperExamId);
        }
        if (paperQuestionId > 0) {
            jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", paperQuestionId);
            jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", paperQuestionId);
        }
        if (extraPaperQuestionId > 0) {
            jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", extraPaperQuestionId);
            jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", extraPaperQuestionId);
        }
        if (questionId > 0) {
            jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", questionId);
            jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", questionId);
        }
        if (studentId > 0) {
            jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", studentId);
        }
        if (subjectId > 0) {
            jdbcTemplate.update("DELETE FROM subject WHERE subject_id = ?", subjectId);
        }
    }

    @Test
    void answerSaveCommitsBeforeWaitingSubmissionAndSubmittedAttemptRejectsLaterSaves() throws Exception {
        LockGate gate = armLockGate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> save = executor.submit(() -> attemptAnswerService.saveAnswer(
                    examId, attemptId, questionId, studentId, new AttemptAnswerRequest(correctAnswerId, null)));
            gate.awaitFirstLock();
            CountDownLatch submissionCompleted = new CountDownLatch(1);
            Future<ExamResultDTO> submission = executor.submit(() -> {
                try {
                    return examSubmissionService.submit(examId, attemptId, studentId);
                } finally {
                    submissionCompleted.countDown();
                }
            });
            gate.awaitSecondAttempt();
            assertThat(submissionCompleted.await(200, TimeUnit.MILLISECONDS))
                    .as("submission must wait for the answer-save transaction").isFalse();

            gate.release();
            save.get(10, TimeUnit.SECONDS);
            assertThat(submission.get(10, TimeUnit.SECONDS).getMarksObtained()).isEqualTo(5);

            assertDatabaseAttemptSubmitted();
            assertThat(selectedAnswerId()).isEqualTo(correctAnswerId);
            assertThat(marksObtained()).isEqualTo(5);
            assertThat(resultCount()).isEqualTo(1);
            activeGate.set(null);
            assertThatThrownBy(() -> attemptAnswerService.saveAnswer(
                    examId, attemptId, questionId, studentId, new AttemptAnswerRequest(incorrectAnswerId, null)))
                    .isInstanceOf(AttemptAnswerException.class)
                    .hasMessage("Exam attempt has already been submitted");
            assertThat(selectedAnswerId()).isEqualTo(correctAnswerId);
            assertThat(resultCount()).isEqualTo(1);
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void saveWaitingBehindSubmissionFailsAndLeavesTheStoredAnswerUnchanged() throws Exception {
        LockGate gate = armLockGate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ExamResultDTO> submission = executor.submit(
                    () -> examSubmissionService.submit(examId, attemptId, studentId));
            gate.awaitFirstLock();
            CountDownLatch saveCompleted = new CountDownLatch(1);
            Future<String> save = executor.submit(() -> {
                try {
                    try {
                        attemptAnswerService.saveAnswer(examId, attemptId, questionId, studentId,
                                new AttemptAnswerRequest(correctAnswerId, null));
                        return "unexpected success";
                    } catch (AttemptAnswerException ex) {
                        return ex.getMessage();
                    }
                } finally {
                    saveCompleted.countDown();
                }
            });
            gate.awaitSecondAttempt();
            assertThat(saveCompleted.await(200, TimeUnit.MILLISECONDS))
                    .as("save must wait for the submission transaction").isFalse();

            gate.release();
            assertThat(submission.get(10, TimeUnit.SECONDS).getMarksObtained()).isZero();
            assertThat(save.get(10, TimeUnit.SECONDS)).isEqualTo("Exam attempt has already been submitted");

            assertDatabaseAttemptSubmitted();
            assertThat(selectedAnswerId()).isEqualTo(incorrectAnswerId);
            assertThat(marksObtained()).isZero();
            assertThat(resultCount()).isEqualTo(1);
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void submissionFailureRollsBackScoringResultAndAttemptStatusAndRetrySucceeds() {
        AtomicBoolean injectFailureOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (injectFailureOnce.getAndSet(false)) {
                throw new IllegalStateException("Controlled failure before attempt finalization");
            }
            return invocation.callRealMethod();
        }).when(examAttemptRepository).finalizeAttempt(anyInt(), org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> examSubmissionService.submit(examId, attemptId, studentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Controlled failure before attempt finalization");

        assertThat(attemptStatus()).isEqualTo(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS);
        assertThat(resultCount()).isZero();
        assertThat(marksObtained()).isNull();
        ExamResultDTO retryResult = examSubmissionService.submit(examId, attemptId, studentId);
        assertThat(retryResult.getMarksObtained()).isEqualTo(0);
        assertDatabaseAttemptSubmitted();
        assertThat(marksObtained()).isZero();
        assertThat(resultCount()).isEqualTo(1);
    }

    @Test
    void concurrentFinalizerRunsCreateOneResultAndKeepSubmissionIdempotent() throws Exception {
        expireAttempt();
        LockGate gate = armLockGate();
        ExpiredAttemptFinalizer finalizer = new ExpiredAttemptFinalizer(
                examAttemptRepository, examSubmissionService, attemptExpiryPolicy);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(finalizer::finalizeExpiredAttempts);
            gate.awaitFirstLock();
            CountDownLatch secondCompleted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                try {
                    finalizer.finalizeExpiredAttempts();
                } finally {
                    secondCompleted.countDown();
                }
            });
            gate.awaitSecondAttempt();
            assertThat(secondCompleted.await(200, TimeUnit.MILLISECONDS))
                    .as("second finalizer must wait on the attempt row lock").isFalse();

            gate.release();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            assertDatabaseAttemptSubmitted();
            assertThat(resultCount()).isEqualTo(1);
            assertThat(marksObtained()).isZero();
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void userSubmissionRacingWithExpiredAttemptFinalizerPersistsOneResult() throws Exception {
        expireAttempt();
        LockGate gate = armLockGate();
        ExpiredAttemptFinalizer finalizer = new ExpiredAttemptFinalizer(
                examAttemptRepository, examSubmissionService, attemptExpiryPolicy);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ExamResultDTO> userSubmission = executor.submit(
                    () -> examSubmissionService.submit(examId, attemptId, studentId));
            gate.awaitFirstLock();
            CountDownLatch finalizationCompleted = new CountDownLatch(1);
            Future<?> finalization = executor.submit(() -> {
                try {
                    finalizer.finalizeExpiredAttempts();
                } finally {
                    finalizationCompleted.countDown();
                }
            });
            gate.awaitSecondAttempt();
            assertThat(finalizationCompleted.await(200, TimeUnit.MILLISECONDS))
                    .as("finalizer must wait on the user's submission lock").isFalse();

            gate.release();
            assertThat(userSubmission.get(10, TimeUnit.SECONDS).getMarksObtained()).isZero();
            finalization.get(10, TimeUnit.SECONDS);

            assertDatabaseAttemptSubmitted();
            assertThat(resultCount()).isEqualTo(1);
            assertThat(marksObtained()).isZero();
            assertThat(examSubmissionService.submit(examId, attemptId, studentId).getMarksObtained()).isZero();
            assertThat(resultCount()).isEqualTo(1);
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void approvalRacingWithQuestionAssignmentNeverAllowsPostApprovalMutation() throws Exception {
        createPendingPaperFixture();
        ExamLockGate gate = armExamLockGate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> approval = executor.submit(() -> examService.acceptExam(paperExamId));
            gate.awaitFirstLock();
            CountDownLatch attachCompleted = new CountDownLatch(1);
            Future<String> attach = executor.submit(() -> {
                try {
                    questionService.attachQuestionsToExam(paperExamId, java.util.List.of(extraPaperQuestionId));
                    return "attached";
                } catch (ExamInUseException ex) {
                    return "rejected";
                } finally {
                    attachCompleted.countDown();
                }
            });
            gate.awaitSecondAttempt();
            assertThat(attachCompleted.await(200, TimeUnit.MILLISECONDS))
                    .as("assignment must wait for the approval transaction's exam lock").isFalse();

            gate.release();
            assertThat(approval.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(attach.get(10, TimeUnit.SECONDS)).isEqualTo("rejected");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM exam WHERE exam_id = ?", String.class, paperExamId))
                    .isEqualTo(ExamConstants.EXAM_STATUS_UPCOMING);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT paper_frozen FROM exam WHERE exam_id = ?", Boolean.class, paperExamId)).isTrue();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM exam_question WHERE exam_id = ? AND question_id = ?",
                    Integer.class, paperExamId, paperQuestionId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM exam_question WHERE exam_id = ? AND question_id = ?",
                    Integer.class, paperExamId, extraPaperQuestionId)).isZero();
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void approvalRacingWithQuestionEditNeverAllowsPostApprovalMutation() throws Exception {
        createPendingPaperFixture();
        ExamLockGate gate = armExamLockGate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> approval = executor.submit(() -> examService.acceptExam(paperExamId));
            gate.awaitFirstLock();
            QuestionDTO edit = QuestionDTO.builder()
                    .questionId(paperQuestionId)
                    .question("Question changed during approval")
                    .subjectId(subjectId)
                    .questionType(ExamConstants.QUESTION_TYPE_MCQ)
                    .marks(5)
                    .option1("Changed correct " + paperQuestionId)
                    .option2("Changed wrong " + paperQuestionId)
                    .correctOption("Changed correct " + paperQuestionId)
                    .build();
            Future<String> update = executor.submit(() -> {
                try {
                    questionService.updateQuestion(edit);
                    return "updated";
                } catch (ExamInUseException ex) {
                    return "rejected";
                }
            });
            gate.awaitSecondAttempt();

            gate.release();
            assertThat(approval.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(update.get(10, TimeUnit.SECONDS)).isEqualTo("rejected");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT question_text FROM question WHERE question_id = ?",
                    String.class, paperQuestionId)).isEqualTo("Existing question");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT option_text FROM answer WHERE question_id = ? AND is_correct = 1",
                    String.class, paperQuestionId)).isEqualTo("Correct " + paperQuestionId);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT paper_frozen FROM exam WHERE exam_id = ?", Boolean.class, paperExamId)).isTrue();
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void attemptCreationRacingWithQuestionAssignmentPreservesTheFrozenPaper() throws Exception {
        createPendingPaperFixture();
        LocalDate today = LocalDate.now();
        jdbcTemplate.update("UPDATE exam SET status = ?, paper_frozen = TRUE, exam_date = ?, "
                        + "start_time = ?, end_time = ? WHERE exam_id = ?",
                ExamConstants.EXAM_STATUS_UPCOMING,
                today.atStartOfDay(),
                LocalTime.now().minusHours(1), LocalTime.now().plusHours(4), paperExamId);
        jdbcTemplate.update("INSERT INTO exam_registration (student_id, exam_id, status) "
                + "VALUES (?, ?, 'Registered')", studentId, paperExamId);
        ExamLockGate gate = armExamLockGate();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> start = executor.submit(() -> {
                var result = examService.startAttempt(paperExamId, studentId);
                paperAttemptId = result.getAttempt().getAttemptId();
            });
            gate.awaitFirstLock();
            Future<String> attach = executor.submit(() -> {
                try {
                    questionService.attachQuestionsToExam(paperExamId,
                            java.util.List.of(extraPaperQuestionId));
                    return "attached";
                } catch (ExamInUseException ex) {
                    return "rejected";
                }
            });
            gate.awaitSecondAttempt();

            gate.release();
            start.get(10, TimeUnit.SECONDS);
            assertThat(attach.get(10, TimeUnit.SECONDS)).isEqualTo("rejected");
            assertThat(paperAttemptId).isPositive();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM exam_question WHERE exam_id = ? AND question_id = ?",
                    Integer.class, paperExamId, extraPaperQuestionId)).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM exam WHERE exam_id = ?", String.class, paperExamId))
                    .isEqualTo(ExamConstants.EXAM_STATUS_UPCOMING);
        } finally {
            gate.release();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void createPendingPaperFixture() {
        LocalDate examDate = LocalDate.now().plusDays(30);
        paperExamId = insertAndGetId("INSERT INTO exam "
                        + "(subject_id, exam_name, exam_type, exam_date, start_time, end_time, duration_minutes, "
                        + "total_marks, pass_marks, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                subjectId, "Paper Freeze Race", ExamConstants.QUESTION_TYPE_MCQ,
                examDate.atTime(10, 0),
                LocalTime.of(10, 0), LocalTime.of(12, 0), 60, 10, 5,
                ExamConstants.EXAM_STATUS_PENDING);
        paperQuestionId = insertValidMcq("Existing question");
        extraPaperQuestionId = insertValidMcq("Question to attach");
        jdbcTemplate.update("INSERT INTO exam_question (exam_id, question_id, question_order) VALUES (?, ?, 1)",
                paperExamId, paperQuestionId);
    }

    private int insertValidMcq(String text) {
        int id = insertAndGetId("INSERT INTO question (subject_id, question_text, question_type, marks) "
                        + "VALUES (?, ?, ?, ?)",
                subjectId, text, ExamConstants.QUESTION_TYPE_MCQ, 5);
        insertAndGetId("INSERT INTO answer (question_id, option_text, is_correct) VALUES (?, ?, ?)",
                id, "Correct " + id, true);
        insertAndGetId("INSERT INTO answer (question_id, option_text, is_correct) VALUES (?, ?, ?)",
                id, "Wrong " + id, false);
        return id;
    }

    private void installExamLockGate() {
        doAnswer(invocation -> {
            ExamLockGate gate = activeExamGate.get();
            int invocationNumber = gate == null ? 0 : gate.beforeLock();
            Object exam = invocation.callRealMethod();
            if (gate != null) {
                gate.afterLock(invocationNumber);
            }
            return exam;
        }).when(examRepository).getExamByIdForUpdate(anyInt());
    }

    private ExamLockGate armExamLockGate() {
        ExamLockGate gate = new ExamLockGate();
        activeExamGate.set(gate);
        return gate;
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Opt-in MySQL integration tests require " + name
                    + " to be set; use a dedicated skillcheckr_it_<suffix> database");
        }
        return value;
    }

    private void installLockGate() {
        doAnswer(invocation -> {
            LockGate gate = activeGate.get();
            int invocationNumber = gate == null ? 0 : gate.beforeLock();
            Object attempt = invocation.callRealMethod();
            if (gate != null) {
                gate.afterLock(invocationNumber);
            }
            return attempt;
        }).when(examAttemptRepository).findByIdForUpdate(anyInt());
    }

    private LockGate armLockGate() {
        LockGate gate = new LockGate();
        activeGate.set(gate);
        return gate;
    }

    private void expireAttempt() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("UPDATE exam_attempt SET expires_at = ? WHERE attempt_id = ?",
                now.minusSeconds(1), attemptId);
        jdbcTemplate.update("UPDATE exam SET exam_date = ?, start_time = ?, end_time = ? WHERE exam_id = ?",
                now.minusHours(2),
                now.minusHours(2).toLocalTime(), now.minusHours(1).toLocalTime(), examId);
    }

    private int insertAndGetId(String sql, Object... values) {
        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder =
                new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL did not return the inserted fixture ID");
        }
        return key.intValue();
    }

    private void assertDatabaseAttemptSubmitted() {
        assertThat(attemptStatus()).isEqualTo(ExamConstants.ATTEMPT_STATUS_SUBMITTED);
    }

    private String attemptStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM exam_attempt WHERE attempt_id = ?",
                String.class, attemptId);
    }

    private Integer selectedAnswerId() {
        return jdbcTemplate.queryForObject("SELECT selected_answer_id FROM attempt_answer "
                + "WHERE attempt_id = ? AND question_id = ?", Integer.class, attemptId, questionId);
    }

    private Integer marksObtained() {
        return jdbcTemplate.queryForObject("SELECT marks_obtained FROM attempt_answer "
                + "WHERE attempt_id = ? AND question_id = ?", Integer.class, attemptId, questionId);
    }

    private int resultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM result WHERE attempt_id = ?",
                Integer.class, attemptId);
    }

    private final class LockGate {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch firstLockAcquired = new CountDownLatch(1);
        private final CountDownLatch secondLockAttempted = new CountDownLatch(1);
        private final CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        private final AtomicBoolean firstReleased = new AtomicBoolean();

        private int beforeLock() {
            int invocation = calls.incrementAndGet();
            if (invocation == 2) {
                secondLockAttempted.countDown();
            }
            return invocation;
        }

        private void afterLock(int invocation) {
            if (invocation != 1) {
                return;
            }
            firstLockAcquired.countDown();
            await(releaseFirstTransaction, "Timed out waiting to release the first transaction");
        }

        private void awaitFirstLock() throws InterruptedException {
            assertThat(firstLockAcquired.await(10, TimeUnit.SECONDS))
                    .as("first transaction must acquire the database row lock").isTrue();
        }

        private void awaitSecondAttempt() throws InterruptedException {
            assertThat(secondLockAttempted.await(10, TimeUnit.SECONDS))
                    .as("second transaction must reach the row-lock read").isTrue();
        }

        private void release() {
            if (firstReleased.compareAndSet(false, true)) {
                releaseFirstTransaction.countDown();
            }
        }

        private void await(CountDownLatch latch, String message) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(message);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while coordinating MySQL transactions", ex);
            }
        }

    }

    private final class ExamLockGate {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch firstLockAcquired = new CountDownLatch(1);
        private final CountDownLatch secondLockAttempted = new CountDownLatch(1);
        private final CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        private final AtomicBoolean firstReleased = new AtomicBoolean();

        private int beforeLock() {
            int invocation = calls.incrementAndGet();
            if (invocation == 2) {
                secondLockAttempted.countDown();
            }
            return invocation;
        }

        private void afterLock(int invocation) {
            if (invocation != 1) {
                return;
            }
            firstLockAcquired.countDown();
            await(releaseFirstTransaction, "Timed out waiting to release the approval or assignment transaction");
        }

        private void awaitFirstLock() throws InterruptedException {
            assertThat(firstLockAcquired.await(10, TimeUnit.SECONDS))
                    .as("first transaction must acquire the exam row lock").isTrue();
        }

        private void awaitSecondAttempt() throws InterruptedException {
            assertThat(secondLockAttempted.await(10, TimeUnit.SECONDS))
                    .as("second transaction must reach the exam-row lock").isTrue();
        }

        private void release() {
            if (firstReleased.compareAndSet(false, true)) {
                releaseFirstTransaction.countDown();
            }
        }

        private void await(CountDownLatch latch, String message) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(message);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while coordinating MySQL exam locks", ex);
            }
        }
    }
}
