package com.example.demo.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.demo.Model.QuestionsDT;

@Repository
public class CreateExamsRepositoryImpl implements CreateExamRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveQuestionWithAnswers(List<QuestionsDT> questions) {
        for (QuestionsDT dto : questions) {
            String insertQuestionSql = "INSERT INTO question(subject_id, question_text) VALUES(?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, dto.getSubjectId());
                ps.setString(2, dto.getQuestion());
                return ps;
            }, keyHolder);

            int questionId = keyHolder.getKey().intValue();

            saveOption(dto.getOption1(), dto.getCorrectOption(), questionId);
            saveOption(dto.getOption2(), dto.getCorrectOption(), questionId);
            saveOption(dto.getOption3(), dto.getCorrectOption(), questionId);
            saveOption(dto.getOption4(), dto.getCorrectOption(), questionId);
        }
    }

    private void saveOption(String option, String correctOption, int questionId) {
        String insertAnswerSql = "INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)";
        boolean isCorrect = option.equals(correctOption);
        jdbcTemplate.update(insertAnswerSql, questionId, option, isCorrect);
    }
}
