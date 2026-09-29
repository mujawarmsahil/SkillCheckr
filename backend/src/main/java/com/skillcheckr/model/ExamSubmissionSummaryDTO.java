package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One student's submission for an exam, as shown to the teacher who owns the exam.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamSubmissionSummaryDTO {

    @JsonProperty("attempt_id")
    @JsonAlias({"attemptId"})
    private int attemptId;

    @JsonProperty("student_id")
    @JsonAlias({"studentId"})
    private int studentId;

    @JsonProperty("student_name")
    @JsonAlias({"studentName"})
    private String studentName;

    @JsonProperty("student_email")
    @JsonAlias({"studentEmail"})
    private String studentEmail;

    @JsonProperty("attempt_status")
    @JsonAlias({"attemptStatus"})
    private String attemptStatus;

    @JsonProperty("result_status")
    @JsonAlias({"resultStatus"})
    private String resultStatus;

    @JsonProperty("marks_obtained")
    @JsonAlias({"marksObtained"})
    private int marksObtained;

    @JsonProperty("total_marks")
    @JsonAlias({"totalMarks"})
    private int totalMarks;

    @JsonProperty("passing_marks")
    @JsonAlias({"passingMarks"})
    private int passingMarks;

    private double percentage;

    @JsonProperty("pending_evaluation")
    @JsonAlias({"pendingEvaluation"})
    private boolean pendingEvaluation;

    @JsonProperty("started_at")
    @JsonAlias({"startedAt"})
    private String startedAt;

    @JsonProperty("submitted_at")
    @JsonAlias({"submittedAt"})
    private String submittedAt;
}
