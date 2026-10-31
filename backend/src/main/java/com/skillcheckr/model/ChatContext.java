package com.skillcheckr.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatContext {

    private Integer examId;

    private Integer questionId;

    private String examName;

    private String subjectName;

    private String examStatus;

    private String examType;

    private Integer durationMinutes;

    private Integer totalMarks;

    private Integer passingMarks;

    private String question;

    private String questionType;

    private List<String> options;

    private Integer marks;

    private Integer wordLimit;

    private String sampleAnswer;

    private List<QuestionDTO> questions;

    public static ChatContext empty() {
        return new ChatContext();
    }
}
