package com.skillcheckr.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.mapper.AttemptAnswerRowMapper;

@Repository
public class AttemptAnswerRepositoryImpl implements AttemptAnswerRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttemptAnswerRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AttemptAnswer> findByAttemptId(int attemptId) {
        String sql = "SELECT aa.attempt_answer_id, aa.attempt_id, aa.question_id, aa.selected_answer_id, "
                + "aa.text_answer, aa.marks_obtained, a.option_text, a.is_correct "
                + "FROM attempt_answer aa LEFT JOIN answer a ON a.answer_id = aa.selected_answer_id "
                + "WHERE aa.attempt_id = ? ORDER BY aa.question_id";
        return jdbcTemplate.query(sql, AttemptAnswerRowMapper.INSTANCE, attemptId);
    }

    @Override
    public boolean answerBelongsToQuestion(int answerId, int questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answer WHERE answer_id = ? AND question_id = ?",
                Integer.class, answerId, questionId);
        return count != null && count == 1;
    }

    @Override
    public AttemptAnswer saveAndReturn(AttemptAnswer answer) {
        String sql = "INSERT INTO attempt_answer "
                + "(attempt_id, question_id, selected_answer_id, text_answer, marks_obtained) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE selected_answer_id = VALUES(selected_answer_id), "
                + "text_answer = VALUES(text_answer), marks_obtained = VALUES(marks_obtained)";
        jdbcTemplate.update(sql,
                answer.getAttempt().getAttemptId(),
                answer.getQuestion().getQuestionId(),
                answer.getSelectedAnswer() == null ? null : answer.getSelectedAnswer().getAnswerId(),
                answer.getTextAnswer(),
                answer.getMarksObtained());
        Integer answerId = jdbcTemplate.queryForObject(
                "SELECT attempt_answer_id FROM attempt_answer WHERE attempt_id = ? AND question_id = ?",
                Integer.class, answer.getAttempt().getAttemptId(), answer.getQuestion().getQuestionId());
        answer.setAttemptAnswerId(answerId == null ? 0 : answerId);
        return answer;
    }

    @Override
    public boolean updateMarks(int attemptId, int questionId, Integer marksObtained) {
        return jdbcTemplate.update("UPDATE attempt_answer SET marks_obtained = ? "
                + "WHERE attempt_id = ? AND question_id = ?", marksObtained, attemptId, questionId) > 0;
    }
}
