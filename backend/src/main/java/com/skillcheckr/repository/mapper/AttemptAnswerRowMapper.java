package com.skillcheckr.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.Answer;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.Question;

public class AttemptAnswerRowMapper implements RowMapper<AttemptAnswer> {

    public static final AttemptAnswerRowMapper INSTANCE = new AttemptAnswerRowMapper();

    @Override
    public AttemptAnswer mapRow(ResultSet rs, int rowNum) throws SQLException {
        Answer selectedAnswer = null;
        if (rs.getObject("selected_answer_id") != null) {
            selectedAnswer = Answer.builder()
                    .answerId(rs.getInt("selected_answer_id"))
                    .optionText(rs.getString("option_text"))
                    .correct(rs.getBoolean("is_correct"))
                    .build();
        }

        return AttemptAnswer.builder()
                .attemptAnswerId(rs.getInt("attempt_answer_id"))
                .question(Question.builder().questionId(rs.getInt("question_id")).build())
                .selectedAnswer(selectedAnswer)
                .textAnswer(rs.getString("text_answer"))
                .marksObtained((Integer) rs.getObject("marks_obtained"))
                .build();
    }
}
