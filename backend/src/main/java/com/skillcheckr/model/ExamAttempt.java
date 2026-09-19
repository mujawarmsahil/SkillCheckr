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
public class ExamAttempt {

    private int attemptId;
    private Exam exam;
    private Student student;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime submittedAt;
    private String status;
}
