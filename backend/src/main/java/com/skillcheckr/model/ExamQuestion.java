package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamQuestion {

    private int examQuestionId;
    private Exam exam;
    private Question question;
    private int questionOrder;
}
