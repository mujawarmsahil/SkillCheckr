package com.skillcheckr.exception;

public class AttemptAnswerException extends AttemptException {

    public AttemptAnswerException(int status, String message) {
        super(status, message);
    }
}