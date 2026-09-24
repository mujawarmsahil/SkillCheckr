package com.skillcheckr.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Student;

public class ExamAttemptRowMapper implements RowMapper<ExamAttempt> {

    public static final ExamAttemptRowMapper INSTANCE = new ExamAttemptRowMapper();

    @Override
    public ExamAttempt mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp started = rs.getTimestamp("started_at");
        Timestamp expires = rs.getTimestamp("expires_at");
        Timestamp submitted = rs.getTimestamp("submitted_at");

        Exam exam = null;
        try {
            int examId = rs.getInt("exam_id");
            if (examId > 0) {
                exam = Exam.builder().examId(examId).build();
            }
        } catch (SQLException ignored) {
            // column not present in some projections
        }

        Student student = null;
        try {
            int studentId = rs.getInt("student_id");
            if (studentId > 0) {
                student = Student.builder().studentId(studentId).build();
            }
        } catch (SQLException ignored) {
            // column not present in some projections
        }

        return ExamAttempt.builder()
                .attemptId(rs.getInt("attempt_id"))
                .exam(exam)
                .student(student)
                .startedAt(toLocalDateTime(started))
                .expiresAt(toLocalDateTime(expires))
                .submittedAt(toLocalDateTime(submitted))
                .status(rs.getString("status"))
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
