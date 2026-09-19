package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttemptStartResult {

    private final ExamAttempt attempt;
    private final boolean existing;
}
