package com.skillcheckr.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Result {

    private int resultId;
    private Exam exam;
    private Student student;
    private int examId;
    private int studentId;
    private int marksObtained;
    private int totalMarks;
    private int passingMarks;
    private double percentage;
    private String status;
    private LocalDateTime submittedAt;
    private ExamAttempt attempt;
}
