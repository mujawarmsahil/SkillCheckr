package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    @JsonProperty("message")
    @JsonAlias({"question", "text"})
    private String message;

    @JsonProperty("examId")
    @JsonAlias({"exam_id"})
    private Integer examId;

    @JsonProperty("questionId")
    @JsonAlias({"question_id"})
    private Integer questionId;
}
