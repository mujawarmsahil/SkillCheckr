package com.skillcheckr.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.ExamResultDTO;

public class ExamResultRowMapper implements RowMapper<ExamResultDTO> {

    public static final ExamResultRowMapper INSTANCE = new ExamResultRowMapper();

    @Override
    public ExamResultDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        String examType = null;
        try {
            examType = rs.getString("exam_type");
        } catch (SQLException ignored) {}
        if (examType == null || examType.isBlank()) {
            examType = ExamConstants.QUESTION_TYPE_MCQ;
        }

        String studentName = null;
        try {
            studentName = rs.getString("student_name");
        } catch (SQLException ignored) {}

        int studentId = rs.getInt("student_id");
        if (studentName == null) {
            studentName = "Student #" + studentId;
        }

        String examName = null;
        try {
            examName = rs.getString("exam_name");
        } catch (SQLException ignored) {}

        String subjectName = null;
        try {
            subjectName = rs.getString("subject_name");
        } catch (SQLException ignored) {}

        int attemptId = 0;
        try {
            attemptId = rs.getInt("attempt_id");
        } catch (SQLException ignored) {}

        return ExamResultDTO.builder()
                .resultId(rs.getInt("result_id"))
                .examId(rs.getInt("exam_id"))
                .attemptId(attemptId)
                .examName(examName)
                .examType(examType)
                .subjectName(subjectName)
                .studentId(studentId)
                .studentName(studentName)
                .marksObtained(rs.getInt("marks_obtained"))
                .totalMarks(rs.getInt("total_marks"))
                .passingMarks(rs.getInt("passing_marks"))
                .percentage(rs.getDouble("percentage"))
                .status(rs.getString("status"))
                .submittedAt(rs.getString("submitted_at"))
                .build();
    }
}
