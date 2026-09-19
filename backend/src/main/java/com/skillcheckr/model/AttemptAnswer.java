package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttemptAnswer {

    private int attemptAnswerId;
    private ExamAttempt attempt;
    private Question question;
    private Answer selectedAnswer;
    private String textAnswer;
    private Integer marksObtained;
}
