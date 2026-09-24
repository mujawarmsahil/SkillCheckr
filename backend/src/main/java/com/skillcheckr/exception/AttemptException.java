package com.skillcheckr.exception;

import lombok.Getter;

@Getter
public abstract class AttemptException extends RuntimeException {

    private final int status;

    public AttemptException(int status, String message) {
        super(message);
        this.status = status;
    }
}