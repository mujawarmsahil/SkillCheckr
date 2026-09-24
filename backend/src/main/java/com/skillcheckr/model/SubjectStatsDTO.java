package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectStatsDTO {

    private int subjectId;

    private String subjectName;

    private String subjectCode;

    private long questionCount;

    private long examCount;
}