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
public class ExamRegistration {

    @JsonProperty("registration_id")
    @JsonAlias({"registrationId", "id"})
    private int registrationId;

    @JsonProperty("student_id")
    @JsonAlias({"studentId"})
    private int studentId;

    @JsonProperty("exam_id")
    @JsonAlias({"examId"})
    private int examId;

    @JsonProperty("registered_at")
    @JsonAlias({"registeredAt"})
    private String registeredAt;

    private String status;

    private Exam exam;
    private Student student;
}
