package com.skillcheckr.service;

import lombok.Getter;

@Getter
public class AttemptAnswerException extends RuntimeException {

    private final int status;

    public AttemptAnswerException(int status, String message) {
        super(message);
        this.status = status;
    }
}
