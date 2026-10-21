package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.model.Subject;

/**
 * Runs the repository against the H2 (MySQL-mode) schema generated for the test context.
 * The child tables (question, answer, exam, exam_question, exam_attempt) have no entities
 * yet, so they are created manually here to exercise the retained native SQL: the
 * random-suffix uniqueness fallback, the attempts guard, and the FK-safe delete cascade.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(SubjectRepositoryImpl.class)
class SubjectPersistenceIntegrationTest {

    @Autowired
    private SubjectRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createChildTables() {
        jdbcTemplate.execute("SET NON_KEYWORDS EXAM");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_question");
        jdbcTemplate.execute("DROP TABLE IF EXISTS answer");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_attempt");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam");
        jdbcTemplate.execute("DROP TABLE IF EXISTS question");
        jdbcTemplate.execute("CREATE TABLE question ("
                + "question_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "subject_id INT NOT NULL,"
                + "question_text VARCHAR(500) NOT NULL,"
                + "CONSTRAINT fk_question_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id))");
        jdbcTemplate.execute("CREATE TABLE answer ("
                + "answer_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "question_id INT NOT NULL,"
                + "option_text VARCHAR(500) NOT NULL,"
                + "CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question (question_id))");
        jdbcTemplate.execute("CREATE TABLE exam ("
                + "exam_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "subject_id INT NOT NULL,"
                + "exam_name VARCHAR(150) NOT NULL,"
                + "CONSTRAINT fk_exam_subject FOREIGN KEY (subject_id) REFERENCES subject (subject_id))");
        jdbcTemplate.execute("CREATE TABLE exam_question ("
                + "exam_question_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "exam_id INT NOT NULL,"
                + "question_id INT NOT NULL,"
                + "CONSTRAINT fk_exam_question_exam FOREIGN KEY (exam_id) REFERENCES exam (exam_id),"
                + "CONSTRAINT fk_exam_question_question FOREIGN KEY (question_id) REFERENCES question (question_id))");
        jdbcTemplate.execute("CREATE TABLE exam_attempt ("
                + "attempt_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "exam_id INT NOT NULL,"
                + "CONSTRAINT fk_exam_attempt_exam FOREIGN KEY (exam_id) REFERENCES exam (exam_id))");
    }

    @Test
    void addSubject_generatesTheCodeFromTheName_whenNoCodeIsProvided() {
        Subject created = repository.addSubject(new Subject(0, "Mathematics", null));

        assertThat(created.getSubjectId()).isPositive();
        assertThat(created.getSubjectCode()).matches("MATH\\d{3}");
        assertThat(repository.getSubjectById(created.getSubjectId())).contains(created);
    }

    @Test
    void addSubject_appendsARandomSuffix_whenTheCodeAlreadyExists() {
        Subject first = repository.addSubject(new Subject(0, "Maths", "MATH"));

        Subject second = repository.addSubject(new Subject(0, "Advanced Maths", "MATH"));

        assertThat(first.getSubjectCode()).isEqualTo("MATH");
        assertThat(second.getSubjectCode()).matches("MATH-\\d{3}");
        assertThat(repository.getAllSubjects()).hasSize(2);
    }

    @Test
    void deleteSubjectById_refusesWhenAnExamHasRecordedAttempts() {
        Subject subject = repository.addSubject(new Subject(0, "Maths", "MATH"));
        jdbcTemplate.update("INSERT INTO exam (subject_id, exam_name) VALUES (?, 'Algebra')",
                subject.getSubjectId());
        int examId = jdbcTemplate.queryForObject("SELECT MAX(exam_id) FROM exam", Integer.class);
        jdbcTemplate.update("INSERT INTO exam_attempt (exam_id) VALUES (?)", examId);

        assertThatThrownBy(() -> repository.deleteSubjectById(subject.getSubjectId()))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("cannot be deleted");

        assertThat(repository.getSubjectById(subject.getSubjectId())).isPresent();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam WHERE exam_id = ?",
                Integer.class, examId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam_attempt WHERE exam_id = ?",
                Integer.class, examId)).isEqualTo(1);
    }

    @Test
    void deleteSubjectById_deletesExamQuestionsAnswersQuestionsExamsThenTheSubject() {
        Subject subject = repository.addSubject(new Subject(0, "Maths", "MATH"));
        int subjectId = subject.getSubjectId();
        jdbcTemplate.update("INSERT INTO question (subject_id, question_text) VALUES (?, 'What is 2+2?')",
                subjectId);
        int questionId = jdbcTemplate.queryForObject("SELECT MAX(question_id) FROM question", Integer.class);
        jdbcTemplate.update("INSERT INTO answer (question_id, option_text) VALUES (?, '4')", questionId);
        jdbcTemplate.update("INSERT INTO exam (subject_id, exam_name) VALUES (?, 'Algebra')", subjectId);
        int examId = jdbcTemplate.queryForObject("SELECT MAX(exam_id) FROM exam", Integer.class);
        jdbcTemplate.update("INSERT INTO exam_question (exam_id, question_id) VALUES (?, ?)", examId, questionId);

        assertThat(repository.deleteSubjectById(subjectId)).isTrue();

        assertThat(repository.getSubjectById(subjectId)).isEmpty();
        assertThat(count("question", subjectId)).isZero();
        assertThat(count("answer", null)).isZero();
        assertThat(count("exam", subjectId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam_question", Integer.class))
                .isZero();
    }

    private int count(String table, Integer subjectId) {
        if (subjectId == null) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        }
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE subject_id = ?", Integer.class, subjectId);
    }
}
