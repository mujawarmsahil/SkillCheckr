package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subject {

    @JsonProperty("subject_id")
    @JsonAlias({"subject_id", "subjectId", "id"})
    private int subjectId;

    @JsonProperty("subject_name")
    @JsonAlias({"subject_name", "subjectName", "name"})
    private String subjectName;

    @JsonProperty("subject_code")
    @JsonAlias({"subject_code", "subjectCode", "code"})
    private String subjectCode;

    @JsonProperty("subjectId")
    public int getSubjectIdCamel() {
        return subjectId;
    }

    @JsonProperty("subjectName")
    public String getSubjectNameCamel() {
        return subjectName;
    }

    @JsonProperty("subjectCode")
    public String getSubjectCodeCamel() {
        return subjectCode;
    }
}
