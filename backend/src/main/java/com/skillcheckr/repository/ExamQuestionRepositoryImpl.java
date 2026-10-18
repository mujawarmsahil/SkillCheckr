package com.skillcheckr.repository;

import java.util.List;
import java.util.Collections;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.Subject;

@Repository
public class ExamQuestionRepositoryImpl implements ExamQuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExamQuestionRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExamQuestion> findByExamId(int examId) {
        String sql = "SELECT eq.exam_question_id, eq.question_order, "
                + "e.exam_id, e.exam_name, q.question_id, q.subject_id, q.question_text, "
                + "q.question_type, q.marks, q.word_limit "
                + "FROM exam_question eq "
                + "JOIN exam e ON e.exam_id = eq.exam_id "
                + "JOIN question q ON q.question_id = eq.question_id "
                + "WHERE eq.exam_id = ? ORDER BY eq.question_order ASC";
        return jdbcTemplate.query(sql, this::mapExamQuestion, examId);
    }

    @Override
    public List<ExamQuestion> findByExamIds(List<Integer> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(examIds.size(), "?"));
        String sql = "SELECT eq.exam_question_id, eq.question_order, "
                + "e.exam_id, e.exam_name, q.question_id, q.subject_id, q.question_text, "
                + "q.question_type, q.marks, q.word_limit "
                + "FROM exam_question eq JOIN exam e ON e.exam_id = eq.exam_id "
                + "JOIN question q ON q.question_id = eq.question_id "
                + "WHERE eq.exam_id IN (" + placeholders + ") ORDER BY eq.exam_id, eq.question_order ASC";
        return jdbcTemplate.query(sql, this::mapExamQuestion, examIds.toArray());
    }

    private ExamQuestion mapExamQuestion(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Question question = Question.builder()
                    .questionId(rs.getInt("question_id"))
                    .questionText(rs.getString("question_text"))
                    .questionType(rs.getString("question_type"))
                    .marks(rs.getInt("marks"))
                    .wordLimit((Integer) rs.getObject("word_limit"))
                    .subject(Subject.builder().subjectId(rs.getInt("subject_id")).build())
                    .build();
            return ExamQuestion.builder()
                    .examQuestionId(rs.getInt("exam_question_id"))
                    .exam(Exam.builder().examId(rs.getInt("exam_id")).examName(rs.getString("exam_name")).build())
                    .question(question)
                    .questionOrder(rs.getInt("question_order"))
                    .build();
    }

    @Override
    public boolean isQuestionAssigned(int examId, int questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE exam_id = ? AND question_id = ?",
                Integer.class, examId, questionId);
        return count != null && count > 0;
    }

    @Override
    public boolean isQuestionAssignedToAnyExam(int questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_question WHERE question_id = ?", Integer.class, questionId);
        return count != null && count > 0;
    }

    @Override
    public int getNextQuestionOrder(int examId) {
        Integer maxOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(question_order), 0) FROM exam_question WHERE exam_id = ?",
                Integer.class, examId);
        return (maxOrder == null ? 0 : maxOrder) + 1;
    }

    /**
     * Reports whether this call actually inserted a row, so the caller can count newly
     * attached questions accurately. A question that is already in the exam returns false:
     * it is not an error, but no row was added, and reporting true would make the caller
     * claim a question was attached when the exam did not change.
     *
     * <p>The existence check keeps the insert from reaching the unique constraint in the
     * common case; the constraint remains the authority for a concurrent attach.
     */
    @Override
    public boolean attachQuestion(int examId, int questionId, int questionOrder) {
        if (isQuestionAssigned(examId, questionId)) {
            return false;
        }
        return jdbcTemplate.update(
                "INSERT INTO exam_question (exam_id, question_id, question_order) VALUES (?, ?, ?)",
                examId, questionId, questionOrder) > 0;
    }

    @Override
    public List<Integer> findSubjectIdsByQuestionId(int questionId) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT e.subject_id FROM exam e "
                        + "JOIN exam_question eq ON e.exam_id = eq.exam_id "
                        + "WHERE eq.question_id = ?",
                Integer.class, questionId);
    }

    @Override
    public List<Integer> findExamIdsByQuestionId(int questionId) {
        return jdbcTemplate.queryForList(
                "SELECT exam_id FROM exam_question WHERE question_id = ? ORDER BY exam_id",
                Integer.class, questionId);
    }

    @Override
    public boolean isExamReadyForApproval(int examId) {
        String sql = "SELECT CASE WHEN COUNT(eq.question_id) > 0 "
                + "AND SUM(CASE WHEN eq.question_id IS NOT NULL AND ("
                + "q.question_id IS NULL "
                + "OR q.question_text IS NULL "
                + "OR TRIM(q.question_text) = '' "
                + "OR CHAR_LENGTH(q.question_text) > 4000 "
                + "OR q.marks IS NULL OR q.marks < 1 OR q.marks > 100 "
                + "OR q.subject_id IS NULL "
                + "OR q.subject_id <> e.subject_id "
                + "OR (UPPER(TRIM(COALESCE(q.question_type, 'MCQ'))) NOT IN ('MCQ', 'QUESTION_ANSWER')) "
                + "OR (UPPER(TRIM(COALESCE(q.question_type, 'MCQ'))) = 'MCQ' AND ("
                + "q.word_limit IS NOT NULL "
                + "OR COALESCE(a.answer_count, 0) < 2 OR COALESCE(a.answer_count, 0) > 4 "
                + "OR COALESCE(a.answer_count, 0) <> COALESCE(a.distinct_answer_count, 0) "
                + "OR COALESCE(a.correct_answer_count, 0) <> 1 "
                + "OR COALESCE(a.invalid_answer_count, 0) > 0)) "
                + "OR (UPPER(TRIM(COALESCE(q.question_type, 'MCQ'))) = 'QUESTION_ANSWER' AND ("
                + "(q.word_limit IS NOT NULL AND (q.word_limit < 1 OR q.word_limit > 5000)) "
                + "OR COALESCE(a.overlength_answer_count, 0) > 0))) "
                + "THEN 1 ELSE 0 END) = 0 THEN 1 ELSE 0 END "
                + "FROM exam e "
                + "LEFT JOIN exam_question eq ON eq.exam_id = e.exam_id "
                + "LEFT JOIN question q ON q.question_id = eq.question_id "
                + "LEFT JOIN (SELECT ans.question_id, COUNT(*) AS answer_count, "
                + "COUNT(DISTINCT LOWER(TRIM(ans.option_text))) AS distinct_answer_count, "
                + "SUM(CASE WHEN ans.is_correct = 1 THEN 1 ELSE 0 END) AS correct_answer_count, "
                + "SUM(CASE WHEN ans.option_text IS NULL OR TRIM(ans.option_text) = '' "
                + "OR CHAR_LENGTH(ans.option_text) > 500 "
                + "THEN 1 ELSE 0 END) AS invalid_answer_count, "
                + "SUM(CASE WHEN CHAR_LENGTH(ans.option_text) > 5000 THEN 1 ELSE 0 END) "
                + "AS overlength_answer_count "
                + "FROM answer ans JOIN exam_question assigned ON assigned.question_id = ans.question_id "
                + "WHERE assigned.exam_id = ? GROUP BY ans.question_id) a ON a.question_id = q.question_id "
                + "WHERE e.exam_id = ?";
        Integer ready = jdbcTemplate.queryForObject(sql, Integer.class, examId, examId);
        return ready != null && ready == 1;
    }
}
