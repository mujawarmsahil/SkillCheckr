package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttemptAnswerResponse {

    @JsonProperty("attemptAnswerId")
    private int attemptAnswerId;

    @JsonProperty("attemptId")
    private int attemptId;

    @JsonProperty("questionId")
    private int questionId;

    @JsonProperty("selectedAnswerId")
    private Integer selectedAnswerId;

    @JsonProperty("textAnswer")
    private String textAnswer;
}
