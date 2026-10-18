package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
                + "aa.text_answer, aa.marks_obtained, a.option_text, a.is_correct, "
                + "(SELECT c.option_text FROM answer c WHERE c.question_id = aa.question_id "
                + "  AND c.is_correct = 1 ORDER BY c.answer_id ASC LIMIT 1) AS correct_option_text "
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
                + "ON DUPLICATE KEY UPDATE attempt_answer_id = LAST_INSERT_ID(attempt_answer_id), "
                + "selected_answer_id = VALUES(selected_answer_id), "
                + "text_answer = VALUES(text_answer), marks_obtained = VALUES(marks_obtained)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, answer.getAttempt().getAttemptId());
            statement.setInt(2, answer.getQuestion().getQuestionId());
            if (answer.getSelectedAnswer() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, answer.getSelectedAnswer().getAnswerId());
            }
            if (answer.getTextAnswer() == null) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, answer.getTextAnswer());
            }
            if (answer.getMarksObtained() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, answer.getMarksObtained());
            }
            return statement;
        }, keyHolder);
        Number answerId = keyHolder.getKey();
        answer.setAttemptAnswerId(answerId == null ? 0 : answerId.intValue());
        return answer;
    }

    @Override
    public boolean updateMarks(int attemptId, int questionId, Integer marksObtained) {
        return jdbcTemplate.update("UPDATE attempt_answer SET marks_obtained = ? "
                + "WHERE attempt_id = ? AND question_id = ?", marksObtained, attemptId, questionId) > 0;
    }

    @Override
    public boolean hasAttemptHistory(int questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attempt_answer WHERE question_id = ?",
                Integer.class, questionId);
        return count != null && count > 0;
    }
}
