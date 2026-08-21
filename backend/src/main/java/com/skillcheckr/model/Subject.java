package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Subject {
    private int subjectId;
    private String subjectName;
    private String subjectCode;
}
