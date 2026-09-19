package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswerRequest {

    @JsonProperty("selectedAnswerId")
    @JsonAlias("selected_answer_id")
    private Integer selectedAnswerId;

    @JsonProperty("textAnswer")
    @JsonAlias("text_answer")
    private String textAnswer;
}
