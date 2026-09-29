package com.skillcheckr.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    /**
     * Serialized with an explicit offset, for example {@code 2026-09-17T19:00:00+05:30}.
     *
     * <p>A bare {@code LocalDateTime} serializes without any zone information, and every browser
     * then reads that wall-clock value as its own local time, so the same attempt would appear to
     * expire at a different absolute instant for each student. Carrying the offset pins it to one
     * instant, which is what the client countdown and the server-side expiry check must agree on.
     *
     * <p>The zone is the JVM default because that is the zone {@code LocalDateTime.now()} used when
     * the expiry was computed, so the offset always describes the same instant the server enforces.
     */
    @JsonProperty("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime expiresAt;

    @JsonProperty("status")
    private String status;

    /**
     * Builds the wire representation of an attempt. Kept here so the single place that attaches a
     * zone to the stored expiry cannot be bypassed by another caller.
     */
    public static ExamAttemptResponse from(ExamAttempt attempt, int examId) {
        LocalDateTime expiresAt = attempt.getExpiresAt();
        return ExamAttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .examId(examId)
                .startedAt(attempt.getStartedAt())
                .expiresAt(expiresAt == null ? null : expiresAt.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .status(attempt.getStatus())
                .build();
    }
}
