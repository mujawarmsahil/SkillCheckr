package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Answer {

    private int answerId;
    private int questionId;
    private String optionText;
    private boolean correct;
}
