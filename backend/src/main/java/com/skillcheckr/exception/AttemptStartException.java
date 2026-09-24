package com.skillcheckr.exception;

public class AttemptStartException extends AttemptException {

    public AttemptStartException(int status, String message) {
        super(status, message);
    }
}