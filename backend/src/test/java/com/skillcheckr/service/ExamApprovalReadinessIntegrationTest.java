package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.ExamAttemptRepositoryImpl;
import com.skillcheckr.repository.ExamQuestionRepositoryImpl;
import com.skillcheckr.repository.ExamRepositoryImpl;
import com.skillcheckr.repository.QuestionRepositoryImpl;
import com.skillcheckr.repository.AttemptAnswerRepositoryImpl;
import com.skillcheckr.repository.SubjectRepositoryImpl;

@JdbcTest
@Import({ ExamRepositoryImpl.class, ExamAttemptRepositoryImpl.class, ExamQuestionRepositoryImpl.class,
        QuestionRepositoryImpl.class, AttemptAnswerRepositoryImpl.class,
        ExamServiceImpl.class, QuestionServiceImpl.class, AttemptExpiryPolicy.class,
        QuestionPaperMutationPolicy.class, SubjectRepositoryImpl.class, SubjectServiceImpl.class })
class ExamApprovalReadinessIntegrationTest {

    private static final int EXAM_ID = 1;
    private static final int SUBJECT_ID = 1;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubjectService subjectService;

    @MockitoBean
    private ExamSubmissionService examSubmissionService;

    @BeforeEach
    void createSchemaAndPendingExam() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS subject ("
                + "subject_id INT PRIMARY KEY, subject_name VARCHAR(100), subject_code VARCHAR(30))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS exam ("
                + "exam_id INT PRIMARY KEY, subject_id INT NOT NULL, teacher_id INT, "
                + "exam_name VARCHAR(150) NOT NULL, exam_type VARCHAR(50) NOT NULL, "
                + "exam_date TIMESTAMP NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, "
                + "duration_minutes INT NOT NULL, total_marks INT NOT NULL, pass_marks INT NOT NULL, "
                + "paper_frozen BOOLEAN NOT NULL DEFAULT FALSE, "
                + "status VARCHAR(50) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS question ("
                + "question_id INT AUTO_INCREMENT PRIMARY KEY, subject_id INT NOT NULL, question_text VARCHAR(5000), "
                + "question_type VARCHAR(20), marks INT, word_limit INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS answer ("
                + "answer_id INT AUTO_INCREMENT PRIMARY KEY, question_id INT NOT NULL, option_text VARCHAR(6000), "
                + "is_correct TINYINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS exam_question ("
                + "exam_question_id INT AUTO_INCREMENT PRIMARY KEY, exam_id INT NOT NULL, question_id INT NOT NULL, "
                + "question_order INT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS exam_attempt ("
                + "attempt_id INT PRIMARY KEY, exam_id INT NOT NULL, student_id INT NOT NULL, "
                + "started_at TIMESTAMP, expires_at TIMESTAMP, submitted_at TIMESTAMP, status VARCHAR(30))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS attempt_answer ("
                + "attempt_answer_id INT PRIMARY KEY, attempt_id INT NOT NULL, question_id INT NOT NULL)");

        jdbcTemplate.update("DELETE FROM exam_question");
        jdbcTemplate.update("DELETE FROM answer");
        jdbcTemplate.update("DELETE FROM question");
        jdbcTemplate.update("DELETE FROM exam");
        jdbcTemplate.update("DELETE FROM subject");
        jdbcTemplate.update("INSERT INTO subject (subject_id, subject_name, subject_code) VALUES (?, ?, ?)",
                SUBJECT_ID, "Mathematics", "MATH");
        jdbcTemplate.update("INSERT INTO exam (exam_id, subject_id, exam_name, exam_type, exam_date, "
                        + "start_time, end_time, duration_minutes, total_marks, pass_marks, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                EXAM_ID, SUBJECT_ID, "Maths Final", "MCQ", "2035-07-15 09:00:00",
                "09:00:00", "11:00:00", 60, 100, 40, "Pending");
    }

    @Test
    void approvalOfReadyExamPersistsUpcomingStatus() {
        insertValidMcqQuestion(1);

        assertThat(examService.acceptExam(EXAM_ID)).isTrue();
        assertStatus("Upcoming");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paper_frozen FROM exam WHERE exam_id = ?", Boolean.class, EXAM_ID)).isTrue();
    }

    @Test
    void approvalWithoutAssignedQuestionsFailsAndLeavesExamPending() {
        assertApprovalFailsAndLeavesPending(() -> examService.acceptExam(EXAM_ID),
                "at least one valid assigned question");
    }

    @Test
    void approvalWithInvalidConfigurationFailsAndLeavesExamPending() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("UPDATE exam SET pass_marks = 101 WHERE exam_id = ?", EXAM_ID);

        assertApprovalFailsAndLeavesPending(() -> examService.acceptExam(EXAM_ID),
                "Passing marks cannot exceed the total marks");
    }

    @Test
    void approvalWithIncompleteAssignedMcqFailsAndLeavesExamPending() {
        jdbcTemplate.update("INSERT INTO question "
                        + "(question_id, subject_id, question_text, question_type, marks) VALUES (?, ?, ?, ?, ?)",
                1, SUBJECT_ID, "Choose an answer", "MCQ", 5);
        jdbcTemplate.update("INSERT INTO exam_question VALUES (?, ?, ?, ?)", 1, EXAM_ID, 1, 1);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)", 1, 1, "Only one option", true);

        assertApprovalFailsAndLeavesPending(() -> examService.acceptExam(EXAM_ID),
                "at least one valid assigned question");
    }

    @Test
    void genericStatusUpdateCannotBypassReadiness() {
        assertApprovalFailsAndLeavesPending(() -> examService.updateExamStatus(EXAM_ID, "Upcoming"),
                "at least one valid assigned question");
    }

    @Test
    void genericApprovedStatusUpdateCannotBypassReadiness() {
        assertApprovalFailsAndLeavesPending(() -> examService.updateExamStatus(EXAM_ID, "Approved"),
                "at least one valid assigned question");
    }

    @Test
    void validDescriptiveQuestionCanBeApproved() {
        jdbcTemplate.update("INSERT INTO question "
                        + "(question_id, subject_id, question_text, question_type, marks, word_limit) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                1, SUBJECT_ID, "Explain the method", "QUESTION_ANSWER", 10, 500);
        jdbcTemplate.update("INSERT INTO exam_question VALUES (?, ?, ?, ?)", 1, EXAM_ID, 1, 1);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)", 1, 1, "A reference answer", 1);

        assertThat(examService.acceptExam(EXAM_ID)).isTrue();
        assertStatus("Upcoming");
    }

    @Test
    void repeatedApprovalOfAnAlreadyOpenExamRemainsIdempotent() {
        insertValidMcqQuestion(1);

        assertThat(examService.acceptExam(EXAM_ID)).isTrue();
        assertThat(examService.acceptExam(EXAM_ID)).isTrue();
        assertStatus("Upcoming");
    }

    @Test
    void pendingExamWithoutAttemptsAllowsQuestionAssignment() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id = ?", EXAM_ID);

        int attached = questionService.attachQuestionsToExam(EXAM_ID, java.util.List.of(1));

        assertThat(attached).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE exam_id = ? AND question_id = ?",
                Integer.class, EXAM_ID, 1)).isEqualTo(1);
    }

    @Test
    void approvedExamRejectsQuestionAssignmentWithoutChangingAssignments() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("INSERT INTO question "
                        + "(question_id, subject_id, question_text, question_type, marks) VALUES (?, ?, ?, ?, ?)",
                2, SUBJECT_ID, "Another question", "MCQ", 5);
        jdbcTemplate.update("UPDATE exam SET status = 'Upcoming' WHERE exam_id = ?", EXAM_ID);

        assertThatThrownBy(() -> questionService.attachQuestionsToExam(EXAM_ID, java.util.List.of(2)))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE exam_id = ?", Integer.class, EXAM_ID)).isEqualTo(1);
    }

    @Test
    void pendingExamWithAttemptButNoSavedAnswerRejectsQuestionAssignment() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id = ?", EXAM_ID);
        jdbcTemplate.update("INSERT INTO exam_attempt "
                        + "(attempt_id, exam_id, student_id, started_at, expires_at, status) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)",
                1, EXAM_ID, 7, "IN_PROGRESS");

        assertThatThrownBy(() -> questionService.attachQuestionsToExam(EXAM_ID, java.util.List.of(1)))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class)
                .hasMessageContaining("after an attempt exists");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE exam_id = ?", Integer.class, EXAM_ID)).isZero();
    }

    @Test
    void pendingExamWithAnswerHistoryRejectsAssignedQuestionEditWithoutChangingQuestionOrOptions() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("INSERT INTO exam_attempt "
                        + "(attempt_id, exam_id, student_id, started_at, expires_at, status) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)",
                1, EXAM_ID, 7, "IN_PROGRESS");
        jdbcTemplate.update("INSERT INTO attempt_answer (attempt_answer_id, attempt_id, question_id) "
                + "VALUES (?, ?, ?)", 1, 1, 1);
        QuestionDTO edit = QuestionDTO.builder()
                .questionId(1)
                .question("Changed question text")
                .subjectId(SUBJECT_ID)
                .option1("Changed A")
                .option2("Changed B")
                .correctOption("Changed A")
                .build();

        assertThatThrownBy(() -> questionService.updateQuestion(edit))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_text FROM question WHERE question_id = ?", String.class, 1))
                .isEqualTo("Choose an answer");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT option_text FROM answer WHERE question_id = ? AND is_correct = 1",
                String.class, 1)).isEqualTo("Option A");
    }

    @Test
    void questionWithAnswerHistoryCannotBeEditedEvenWithoutAnAttemptOnItsExam() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("INSERT INTO attempt_answer (attempt_answer_id, attempt_id, question_id) "
                + "VALUES (?, ?, ?)", 1, 999, 1);
        QuestionDTO edit = QuestionDTO.builder()
                .questionId(1)
                .question("Changed question text")
                .subjectId(SUBJECT_ID)
                .option1("Changed A")
                .option2("Changed B")
                .correctOption("Changed A")
                .build();

        assertThatThrownBy(() -> questionService.updateQuestion(edit))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class)
                .hasMessageContaining("already been attempted");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_text FROM question WHERE question_id = ?", String.class, 1))
                .isEqualTo("Choose an answer");
    }

    @Test
    void questionUsedByMultipleExamsIsProtectedWhenAnyOneIsApproved() {
        insertValidMcqQuestion(1);
        jdbcTemplate.update("INSERT INTO exam (exam_id, subject_id, exam_name, exam_type, exam_date, "
                        + "start_time, end_time, duration_minutes, total_marks, pass_marks, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                2, SUBJECT_ID, "Second Exam", "MCQ", "2035-07-15 09:00:00",
                "09:00:00", "11:00:00", 60, 100, 40, "Upcoming");
        jdbcTemplate.update("INSERT INTO exam_question VALUES (?, ?, ?, ?)", 2, 2, 1, 1);
        QuestionDTO edit = QuestionDTO.builder()
                .questionId(1)
                .question("Changed question text")
                .subjectId(SUBJECT_ID)
                .option1("Changed A")
                .option2("Changed B")
                .correctOption("Changed A")
                .build();

        assertThatThrownBy(() -> questionService.updateQuestion(edit))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class)
                .hasMessageContaining("paper is frozen");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_text FROM question WHERE question_id = ?", String.class, 1))
                .isEqualTo("Choose an answer");
    }

    @Test
    void unassignedQuestionCanBeEditedWithoutAnswerHistory() {
        jdbcTemplate.update("INSERT INTO question "
                        + "(question_id, subject_id, question_text, question_type, marks) VALUES (?, ?, ?, ?, ?)",
                5, SUBJECT_ID, "Unassigned bank question", "MCQ", 5);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)", 9, 5, "Before", 1);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)", 10, 5, "Other", 0);
        QuestionDTO edit = QuestionDTO.builder()
                .questionId(5)
                .question("Updated bank question")
                .subjectId(SUBJECT_ID)
                .option1("Updated A")
                .option2("Updated B")
                .correctOption("Updated A")
                .build();

        assertThat(questionService.updateQuestion(edit)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question_text FROM question WHERE question_id = ?", String.class, 5))
                .isEqualTo("Updated bank question");
    }

    @Test
    void bulkQuestionCreationCannotAssignToAnApprovedExam() {
        jdbcTemplate.update("UPDATE exam SET status = 'Approved' WHERE exam_id = ?", EXAM_ID);
        QuestionDTO question = QuestionDTO.builder()
                .question("New question")
                .subjectId(SUBJECT_ID)
                .examId(EXAM_ID)
                .option1("A")
                .option2("B")
                .correctOption("A")
                .build();

        assertThatThrownBy(() -> questionService.saveQuestionsWithAnswers(java.util.List.of(question)))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE exam_id = ?", Integer.class, EXAM_ID)).isZero();
    }

    @Test
    void approvedPaperCannotBeReopenedByStatusRollback() {
        insertValidMcqQuestion(1);
        assertThat(examService.acceptExam(EXAM_ID)).isTrue();

        assertThatThrownBy(() -> examService.updateExamStatus(EXAM_ID, "Pending"))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class)
                .hasMessageContaining("cannot return to an unapproved status");
        assertStatus("Upcoming");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paper_frozen FROM exam WHERE exam_id = ?", Boolean.class, EXAM_ID)).isTrue();
    }

    @Test
    void approvedExamCannotBeDeletedThroughSubjectDeletion() {
        insertValidMcqQuestion(1);
        assertThat(examService.acceptExam(EXAM_ID)).isTrue();

        assertThatThrownBy(() -> subjectService.deleteSubjectById(SUBJECT_ID))
                .isInstanceOf(com.skillcheckr.exception.ExamInUseException.class)
                .hasMessageContaining("paper is frozen");
        assertStatus("Upcoming");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subject WHERE subject_id = ?", Integer.class, SUBJECT_ID)).isEqualTo(1);
    }

    private void insertValidMcqQuestion(int questionId) {
        jdbcTemplate.update("INSERT INTO question "
                        + "(question_id, subject_id, question_text, question_type, marks) VALUES (?, ?, ?, ?, ?)",
                questionId, SUBJECT_ID, "Choose an answer", "MCQ", 5);
        jdbcTemplate.update("INSERT INTO exam_question VALUES (?, ?, ?, ?)",
                questionId, EXAM_ID, questionId, 1);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)",
                questionId * 2 - 1, questionId, "Option A", 1);
        jdbcTemplate.update("INSERT INTO answer VALUES (?, ?, ?, ?)",
                questionId * 2, questionId, "Option B", 0);
    }

    private void assertApprovalFailsAndLeavesPending(Runnable approval, String message) {
        assertThatThrownBy(approval::run)
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(message);
        assertStatus("Pending");
    }

    private void assertStatus(String expectedStatus) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM exam WHERE exam_id = ?", String.class, EXAM_ID))
                .isEqualTo(expectedStatus);
    }
}
