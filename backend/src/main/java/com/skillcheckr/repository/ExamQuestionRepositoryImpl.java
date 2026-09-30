package com.skillcheckr.repository;

import java.util.List;

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
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        }, examId);
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
}
