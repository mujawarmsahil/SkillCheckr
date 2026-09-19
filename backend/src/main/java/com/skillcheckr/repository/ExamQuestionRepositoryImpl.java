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
    public boolean assignQuestion(int examId, int questionId, int questionOrder) {
        String sql = "INSERT INTO exam_question (exam_id, question_id, question_order) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, examId, questionId, questionOrder) > 0;
    }
}
