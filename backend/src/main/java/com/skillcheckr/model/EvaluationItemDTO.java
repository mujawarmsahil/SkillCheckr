package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single question of a submitted attempt as shown on the teacher's evaluation screen.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationItemDTO {

    @JsonProperty("question_id")
    @JsonAlias({"questionId"})
    private int questionId;

    @JsonProperty("question_order")
    @JsonAlias({"questionOrder"})
    private int questionOrder;

    private String question;

    @JsonProperty("question_type")
    @JsonAlias({"questionType"})
    private String questionType;

    private int marks;

    @JsonProperty("word_limit")
    @JsonAlias({"wordLimit"})
    private Integer wordLimit;

    @JsonProperty("text_answer")
    @JsonAlias({"textAnswer"})
    private String textAnswer;

    @JsonProperty("selected_answer")
    @JsonAlias({"selectedAnswer"})
    private String selectedAnswer;

    @JsonProperty("awarded_marks")
    @JsonAlias({"awardedMarks"})
    private Integer awardedMarks;

    @JsonProperty("is_evaluated")
    @JsonAlias({"isEvaluated"})
    private boolean evaluated;

    @JsonProperty("requires_evaluation")
    @JsonAlias({"requiresEvaluation"})
    private boolean requiresEvaluation;
}
