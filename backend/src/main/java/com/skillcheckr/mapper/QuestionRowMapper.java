package com.skillcheckr.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.Question;

public class QuestionRowMapper implements RowMapper<Question> {

    public static final QuestionRowMapper INSTANCE = new QuestionRowMapper();

    @Override
    public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Question.builder()
                .questionId(rs.getInt("question_id"))
                .questionText(rs.getString("question_text"))
                .questionType(rs.getString("question_type"))
                .marks(rs.getInt("marks"))
                .wordLimit((Integer) rs.getObject("word_limit"))
                .build();
    }
}
