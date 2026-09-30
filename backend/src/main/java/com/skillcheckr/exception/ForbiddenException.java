package com.skillcheckr.exception;

public class ForbiddenException extends AttemptException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}
