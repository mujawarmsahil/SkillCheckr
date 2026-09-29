package com.skillcheckr.exception;

/**
 * Raised when an operation is refused because existing data would be destroyed.
 */
public class ExamInUseException extends AttemptException {

    public ExamInUseException(String message) {
        super(409, message);
    }
}
