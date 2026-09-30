package com.skillcheckr.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.Question;
import com.skillcheckr.model.Subject;

public class QuestionRowMapper implements RowMapper<Question> {

    public static final QuestionRowMapper INSTANCE = new QuestionRowMapper();

    @Override
    public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
        Subject subject = null;
        if (rs.getObject("subject_id") != null) {
            subject = Subject.builder().subjectId(rs.getInt("subject_id")).build();
        }
        return Question.builder()
                .questionId(rs.getInt("question_id"))
                .subject(subject)
                .questionText(rs.getString("question_text"))
                .questionType(rs.getString("question_type"))
                .marks(rs.getInt("marks"))
                .wordLimit((Integer) rs.getObject("word_limit"))
                .build();
    }
}
