package com.skillcheckr.service;

import lombok.Getter;

@Getter
public class ExamSubmissionException extends RuntimeException {

    private final int status;

    public ExamSubmissionException(int status, String message) {
        super(message);
        this.status = status;
    }
}
