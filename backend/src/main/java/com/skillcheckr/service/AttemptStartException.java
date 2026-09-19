package com.skillcheckr.service;

import lombok.Getter;

@Getter
public class AttemptStartException extends RuntimeException {

    private final int status;

    public AttemptStartException(int status, String message) {
        super(message);
        this.status = status;
    }
}
