package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Question {

    private int questionId;
    private Subject subject;
    private String questionText;
    private String questionType;
    private int marks;
    private Integer wordLimit;
}
