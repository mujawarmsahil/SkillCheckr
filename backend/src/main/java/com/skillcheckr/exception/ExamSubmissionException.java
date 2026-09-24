package com.skillcheckr.exception;

public class ExamSubmissionException extends AttemptException {

    public ExamSubmissionException(int status, String message) {
        super(status, message);
    }
}