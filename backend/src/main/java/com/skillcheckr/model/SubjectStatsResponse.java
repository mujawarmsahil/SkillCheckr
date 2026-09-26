package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectStatsResponse {

    private int subjectId;

    private String subjectName;

    private String subjectCode;

    private int questionCount;

    private int examCount;
}
