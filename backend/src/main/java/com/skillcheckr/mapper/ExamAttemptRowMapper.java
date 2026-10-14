package com.skillcheckr.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Student;

public class ExamAttemptRowMapper implements RowMapper<ExamAttempt> {

    public static final ExamAttemptRowMapper INSTANCE = new ExamAttemptRowMapper();

    @Override
    public ExamAttempt mapRow(ResultSet rs, int rowNum) throws SQLException {
        // started_at, expires_at and submitted_at are MySQL DATETIME wall-clock values, so
        // they are read as LocalDateTime. Reading them as java.sql.Timestamp would apply
        // the connection timezone and shift the stored value.
        LocalDateTime started = rs.getObject("started_at", LocalDateTime.class);
        LocalDateTime expires = rs.getObject("expires_at", LocalDateTime.class);
        LocalDateTime submitted = rs.getObject("submitted_at", LocalDateTime.class);

        int examId = rs.getInt("exam_id");
        Exam exam = examId > 0 ? Exam.builder().examId(examId).build() : null;

        int studentId = rs.getInt("student_id");
        Student student = studentId > 0 ? Student.builder().studentId(studentId).build() : null;

        return ExamAttempt.builder()
                .attemptId(rs.getInt("attempt_id"))
                .exam(exam)
                .student(student)
                .startedAt(started)
                .expiresAt(expires)
                .submittedAt(submitted)
                .status(rs.getString("status"))
                .build();
    }
}
