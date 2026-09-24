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
public class RegistrationStatusResponse {

    @JsonProperty("isRegistered")
    private boolean isRegistered;

    private int examId;

    private int studentId;
}