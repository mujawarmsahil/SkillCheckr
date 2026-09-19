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
public class QuestionDTO {

	@JsonProperty("question_id")
	@JsonAlias({"questionId", "id"})
	private int questionId;

	private String question;

	@JsonProperty("question_type")
	@JsonAlias({"questionType"})
	private String questionType; // "MCQ" or "QUESTION_ANSWER"

	private String option1;
	private String option2;
	private String option3;
	private String option4;

	@JsonProperty("option1Id")
	@JsonAlias({"option1_id"})
	private Integer option1Id;

	@JsonProperty("option2Id")
	@JsonAlias({"option2_id"})
	private Integer option2Id;

	@JsonProperty("option3Id")
	@JsonAlias({"option3_id"})
	private Integer option3Id;

	@JsonProperty("option4Id")
	@JsonAlias({"option4_id"})
	private Integer option4Id;

	@JsonProperty("correct_option")
	@JsonAlias({"correctOption"})
	private String correctOption;

	@JsonProperty("sample_answer")
	@JsonAlias({"sampleAnswer", "idealAnswer", "answerText"})
	private String sampleAnswer;

	@Builder.Default
	private int marks = 1;

	@JsonProperty("word_limit")
	@JsonAlias({"wordLimit"})
	private Integer wordLimit;

	@JsonProperty("subject_id")
	@JsonAlias({"subjectId"})
	private int subjectId;

	@JsonProperty("subject_name")
	@JsonAlias({"subjectName"})
	private String subjectName;

	@JsonProperty("exam_id")
	@JsonAlias({"examId"})
	private Integer examId;
}
