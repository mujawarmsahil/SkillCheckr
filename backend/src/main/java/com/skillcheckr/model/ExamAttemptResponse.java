package com.skillcheckr.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamAttemptResponse {

    @JsonProperty("attemptId")
    private int attemptId;

    @JsonProperty("examId")
    private int examId;

    @JsonProperty("startedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime startedAt;

    @JsonProperty("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime expiresAt;

    @JsonProperty("status")
    private String status;
}
