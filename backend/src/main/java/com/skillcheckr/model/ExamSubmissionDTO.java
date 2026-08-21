package com.skillcheckr.model;

import java.util.Map;

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
public class ExamSubmissionDTO {

	@JsonProperty("exam_id")
	@JsonAlias({"examId"})
	private int examId;

	@JsonProperty("student_id")
	@JsonAlias({"studentId"})
	private int studentId;

	@JsonProperty("student_name")
	@JsonAlias({"studentName"})
	private String studentName;

	@JsonProperty("exam_type")
	@JsonAlias({"examType"})
	private String examType; // "MCQ" or "QUESTION_ANSWER"

	@JsonProperty("mcq_answers")
	@JsonAlias({"mcqAnswers", "answers"})
	private Map<String, String> mcqAnswers; // questionId as String/Integer -> chosen option string

	@JsonProperty("text_answers")
	@JsonAlias({"textAnswers", "descriptiveAnswers"})
	private Map<String, String> textAnswers; // questionId -> written text answer
}
