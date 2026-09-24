package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsResponse {

    private int totalStudents;

    private int totalTeachers;

    private int totalExams;

    private int pendingExams;

    private int upcomingExams;

    private int completedExams;

    private int pendingRequests;

    private int totalSubjects;

    private int totalQuestions;

    private int totalResults;
}