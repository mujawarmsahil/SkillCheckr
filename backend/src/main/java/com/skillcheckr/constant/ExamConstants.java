package com.skillcheckr.constant;

public final class ExamConstants {

    private ExamConstants() {
    }

    // Question Types
    public static final String QUESTION_TYPE_MCQ = "MCQ";
    public static final String QUESTION_TYPE_QUESTION_ANSWER = "QUESTION_ANSWER";

    // Attempt Statuses
    public static final String ATTEMPT_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String ATTEMPT_STATUS_SUBMITTED = "SUBMITTED";

    // Result Statuses
    public static final String RESULT_STATUS_PASS = "Pass";
    public static final String RESULT_STATUS_FAIL = "Fail";
    public static final String RESULT_STATUS_SUBMITTED_FOR_EVALUATION = "Submitted for Evaluation";

    // Exam Statuses
    public static final String EXAM_STATUS_UPCOMING = "Upcoming";
    public static final String EXAM_STATUS_APPROVED = "Approved";
    public static final String EXAM_STATUS_REJECTED = "Rejected";
    public static final String EXAM_STATUS_CANCELLED = "Cancelled";
}
