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
        String examType = rs.getString("exam_type");
        if (examType == null || examType.isBlank()) {
            examType = ExamConstants.QUESTION_TYPE_MCQ;
        }

        int studentId = rs.getInt("student_id");
        String studentName = rs.getString("student_name");
        if (studentName == null) {
            studentName = "Student #" + studentId;
        }

        return ExamResultDTO.builder()
                .resultId(rs.getInt("result_id"))
                .examId(rs.getInt("exam_id"))
                .attemptId(rs.getInt("attempt_id"))
                .examName(rs.getString("exam_name"))
                .examType(examType)
                .subjectName(rs.getString("subject_name"))
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
