package com.skillcheckr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.skillcheckr.model.ExamResultDTO;

class ExamResultRowMapperTest {

    @Test
    void mapsEveryColumnFromTheKnownQueryShape() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getString("student_name")).thenReturn("Asha");

        ExamResultDTO dto = ExamResultRowMapper.INSTANCE.mapRow(rs, 0);

        assertThat(dto.getResultId()).isEqualTo(7);
        assertThat(dto.getExamId()).isEqualTo(3);
        assertThat(dto.getAttemptId()).isEqualTo(11);
        assertThat(dto.getStudentId()).isEqualTo(42);
        assertThat(dto.getStudentName()).isEqualTo("Asha");
        assertThat(dto.getExamName()).isEqualTo("Midterm");
        assertThat(dto.getExamType()).isEqualTo("MCQ");
        assertThat(dto.getSubjectName()).isEqualTo("Maths");
        assertThat(dto.getMarksObtained()).isEqualTo(18);
        assertThat(dto.getTotalMarks()).isEqualTo(20);
        assertThat(dto.getPassingMarks()).isEqualTo(10);
        assertThat(dto.getPercentage()).isEqualTo(90.0);
        assertThat(dto.getStatus()).isEqualTo("Completed");
    }

    @Test
    void fallsBackToAStudentLabelWhenTheProjectedStudentNameIsNull() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getString("student_name")).thenReturn(null);

        ExamResultDTO dto = ExamResultRowMapper.INSTANCE.mapRow(rs, 0);

        assertThat(dto.getStudentName()).isEqualTo("Student #42");
    }

    @Test
    void defaultsABlankExamTypeToMcq() throws SQLException {
        ResultSet rs = baseResultSet();
        when(rs.getString("exam_type")).thenReturn("   ");

        ExamResultDTO dto = ExamResultRowMapper.INSTANCE.mapRow(rs, 0);

        assertThat(dto.getExamType()).isEqualTo("MCQ");
    }

    @Test
    void propagatesFailuresFromRequiredColumns() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("result_id")).thenThrow(new SQLException("connection reset"));

        assertThatThrownBy(() -> ExamResultRowMapper.INSTANCE.mapRow(rs, 0))
                .isInstanceOf(SQLException.class)
                .hasMessage("connection reset");
    }

    private ResultSet baseResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getInt("result_id")).thenReturn(7);
        when(rs.getInt("exam_id")).thenReturn(3);
        when(rs.getInt("attempt_id")).thenReturn(11);
        when(rs.getInt("student_id")).thenReturn(42);
        when(rs.getString("exam_name")).thenReturn("Midterm");
        when(rs.getString("exam_type")).thenReturn("MCQ");
        when(rs.getString("subject_name")).thenReturn("Maths");
        when(rs.getInt("marks_obtained")).thenReturn(18);
        when(rs.getInt("total_marks")).thenReturn(20);
        when(rs.getInt("passing_marks")).thenReturn(10);
        when(rs.getDouble("percentage")).thenReturn(90.0);
        when(rs.getString("status")).thenReturn("Completed");
        return rs;
    }
}
