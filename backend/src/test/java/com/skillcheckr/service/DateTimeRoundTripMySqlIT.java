package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.ResultRepository;

/**
 * Regression coverage for the JDBC timezone defect: MySQL DATETIME columns hold wall-clock
 * values with no zone, so a driver-side conversion between the connection timezone and the
 * JVM zone moved them onto a different calendar day. Every assertion below compares the raw
 * stored column against the intended literal, because a read and a write that are both
 * shifted in opposite directions would still round-trip to the right value in Java while
 * leaving the wrong value in the database.
 */
@SpringBootTest(properties = "app.exam.attempt-finalizer-initial-delay-ms=3600000")
@ActiveProfiles("mysql-it")
class DateTimeRoundTripMySqlIT {

    private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern SAFE_TEST_DATABASE_URL = Pattern.compile(
            "^jdbc:mysql://[^/?#]+/(skillcheckr_it_[a-zA-Z0-9_]+)(?:\\?.*)?$");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ResultRepository resultRepository;

    private int subjectId;
    private int studentId;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        String url = requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_URL");
        Matcher matcher = SAFE_TEST_DATABASE_URL.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalStateException("SKILLCHECKR_MYSQL_IT_URL must point to a dedicated "
                    + "MySQL database named skillcheckr_it_<suffix>; refusing to start integration tests");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironmentVariable("SKILLCHECKR_MYSQL_IT_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.flyway.clean-disabled", () -> "true");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @BeforeEach
    void createFixture() {
        // Logged so a run under an unexpected JVM zone is visible in the build output
        // instead of silently changing what the assertions compare against.
        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO,
                "JVM zone {0}, database {1}",
                java.time.ZoneId.systemDefault(), jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        subjectId = insertAndGetId("INSERT INTO subject (subject_name, subject_code) VALUES (?, ?)",
                "Round Trip IT", "RT-" + System.nanoTime());
        studentId = insertAndGetId("INSERT INTO student (name, email) VALUES (?, ?)",
                "Round Trip Student", "rt-" + System.nanoTime() + "@example.test");
    }

    @AfterEach
    void cleanFixture() {
        jdbcTemplate.update("DELETE FROM result WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM attempt_answer WHERE attempt_id IN "
                + "(SELECT attempt_id FROM exam_attempt WHERE student_id = ?)", studentId);
        jdbcTemplate.update("DELETE FROM exam_attempt WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM exam_question WHERE exam_id IN "
                + "(SELECT exam_id FROM exam WHERE subject_id = ?)", subjectId);
        jdbcTemplate.update("DELETE FROM exam WHERE subject_id = ?", subjectId);
        jdbcTemplate.update("DELETE FROM student WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM subject WHERE subject_id = ?", subjectId);
    }

    @Test
    void examDateAtTheStartOfADayIsStoredOnThatExactCalendarDay() {
        LocalDateTime intended = LocalDate.now().atStartOfDay();

        int examId = saveExam("Midnight Boundary", intended, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(rawExamDate(examId)).isEqualTo(DATETIME_FORMAT.format(intended));
        assertThat(jdbcTemplate.queryForObject("SELECT DATE(exam_date) FROM exam WHERE exam_id = ?",
                LocalDate.class, examId)).isEqualTo(intended.toLocalDate());
    }

    @Test
    void examDateOnTheLastSecondOfAYearKeepsItsYearAndDay() {
        LocalDateTime intended = LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59);

        int examId = saveExam("Year End Boundary", intended, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(rawExamDate(examId)).isEqualTo(DATETIME_FORMAT.format(intended));
    }

    @Test
    void theScheduledWindowIsDerivedFromTheDayThatWasActuallyStored() {
        // Before the fix the JVM converted this wall-clock value to UTC on write, so the
        // stored calendar day was the previous one and the whole window shifted with it.
        LocalDateTime intended = LocalDate.now().atTime(0, 0);
        int examId = saveExam("Window Day", intended, LocalTime.of(9, 0), LocalTime.of(11, 0));

        Exam reloaded = examRepository.getExamById(examId).orElseThrow();

        assertThat(com.skillcheckr.validation.ExamCreationValidator.resolveWindowStart(reloaded))
                .isEqualTo(intended.with(LocalTime.of(9, 0)));
        assertThat(com.skillcheckr.validation.ExamCreationValidator.resolveWindowEnd(reloaded))
                .isEqualTo(intended.with(LocalTime.of(11, 0)));
    }

    @Test
    void attemptWindowColumnsAreStoredAndReadBackVerbatim() {
        LocalDateTime startedAt = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime expiresAt = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(0);

        int examId = saveExam("Attempt Day", LocalDate.now().atStartOfDay(),
                LocalTime.of(9, 0), LocalTime.of(23, 59));
        ExamAttempt attempt = ExamAttempt.builder()
                .exam(Exam.builder().examId(examId).build())
                .student(Student.builder().studentId(studentId).build())
                .startedAt(startedAt)
                .expiresAt(expiresAt)
                .status("IN_PROGRESS")
                .build();
        int attemptId = examAttemptRepository.createAttempt(attempt);

        ExamAttempt reloaded = examAttemptRepository.findById(attemptId);
        assertThat(reloaded.getStartedAt()).isEqualTo(startedAt);
        assertThat(reloaded.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(reloaded.getSubmittedAt()).isNull();
        assertThat(rawAttemptColumn("started_at", attemptId)).isEqualTo(DATETIME_FORMAT.format(startedAt));
        assertThat(rawAttemptColumn("expires_at", attemptId)).isEqualTo(DATETIME_FORMAT.format(expiresAt));
        assertThat(rawAttemptColumn("submitted_at", attemptId)).isNull();
    }

    @Test
    void finalizingAnAttemptStoresTheSubmittedMomentVerbatim() {
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2).withNano(0);
        int examId = saveExam("Submit Day", LocalDate.now().atStartOfDay(), LocalTime.of(9, 0), LocalTime.of(23, 0));
        int attemptId = examAttemptRepository.createAttempt(ExamAttempt.builder()
                .exam(Exam.builder().examId(examId).build())
                .student(Student.builder().studentId(studentId).build())
                .startedAt(startedAt)
                .expiresAt(startedAt.plusHours(2))
                .status("IN_PROGRESS")
                .build());

        LocalDateTime submittedAt = LocalDateTime.now().withNano(0);
        assertThat(examAttemptRepository.finalizeAttempt(attemptId, submittedAt)).isTrue();

        assertThat(rawAttemptColumn("submitted_at", attemptId)).isEqualTo(DATETIME_FORMAT.format(submittedAt));
        assertThat(examAttemptRepository.findById(attemptId).getSubmittedAt()).isEqualTo(submittedAt);
    }

    @Test
    void resultSubmissionMomentIsStoredVerbatim() {
        int examId = saveExam("Result Day", LocalDate.now().atStartOfDay(), LocalTime.of(9, 0), LocalTime.of(23, 0));
        int attemptId = examAttemptRepository.createAttempt(ExamAttempt.builder()
                .exam(Exam.builder().examId(examId).build())
                .student(Student.builder().studentId(studentId).build())
                .startedAt(LocalDateTime.now().minusHours(1).withNano(0))
                .expiresAt(LocalDateTime.now().plusHours(1).withNano(0))
                .status("IN_PROGRESS")
                .build());
        LocalDateTime submittedAt = LocalDateTime.now().withNano(0);

        resultRepository.insertSubmissionResult(ExamResultDTO.builder()
                .examId(examId)
                .studentId(studentId)
                .marksObtained(4)
                .totalMarks(5)
                .passingMarks(3)
                .percentage(80.0)
                .status("Pass")
                .submittedAt(SUBMITTED_AT_FORMAT.format(submittedAt))
                .attemptId(attemptId)
                .build());

        Integer resultId = jdbcTemplate.queryForObject(
                "SELECT result_id FROM result WHERE attempt_id = ?", Integer.class, attemptId);
        assertThat(rawResultColumn("submitted_at", resultId)).isEqualTo(DATETIME_FORMAT.format(submittedAt));
    }

    private int saveExam(String name, LocalDateTime examDate, LocalTime startTime, LocalTime endTime) {
        Subject subject = new Subject();
        subject.setSubjectId(subjectId);
        subject.setSubjectCode(subjectCode());
        subject.setSubjectName("Round Trip IT");
        Exam exam = new Exam();
        exam.setSubject(subject);
        exam.setExamName(name);
        exam.setDate(examDate.toString());
        exam.setStartTime(startTime);
        exam.setEndTime(endTime);
        exam.setDurationMinutes(60);
        exam.setTotalMarks(10);
        exam.setPassingMarks(4);
        exam.setStatus("Pending");
        exam.setTeacherId(0);
        return examRepository.saveExam(exam).getExamId();
    }

    private String subjectCode() {
        return jdbcTemplate.queryForObject("SELECT subject_code FROM subject WHERE subject_id = ?",
                String.class, subjectId);
    }

    private String rawExamDate(int examId) {
        return rawDatetime("exam", "exam_id", "exam_date", examId);
    }

    private String rawAttemptColumn(String column, int attemptId) {
        return rawDatetime("exam_attempt", "attempt_id", column, attemptId);
    }

    private String rawResultColumn(String column, int resultId) {
        return rawDatetime("result", "result_id", column, resultId);
    }

    private String rawDatetime(String table, String idColumn, String column, int rowId) {
        return jdbcTemplate.queryForObject(
                "SELECT CAST(" + column + " AS CHAR) FROM " + table + " WHERE " + idColumn + " = ?",
                String.class, rowId);
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

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Opt-in MySQL integration tests require " + name
                    + " to be set; use a dedicated skillcheckr_it_<suffix> database");
        }
        return value;
    }
}