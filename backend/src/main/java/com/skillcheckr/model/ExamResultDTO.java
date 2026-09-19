package com.skillcheckr.model;

import java.util.List;
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
public class ExamResultDTO {

	@JsonProperty("result_id")
	@JsonAlias({"resultId", "id"})
	private int resultId;

	@JsonProperty("exam_id")
	@JsonAlias({"examId"})
	private int examId;

	@JsonProperty("exam_name")
	@JsonAlias({"examName"})
	private String examName;

	@JsonProperty("exam_type")
	@JsonAlias({"examType"})
	private String examType;

	@JsonProperty("subject_id")
	@JsonAlias({"subjectId"})
	private int subjectId;

	@JsonProperty("subject_name")
	@JsonAlias({"subjectName"})
	private String subjectName;

	@JsonProperty("student_id")
	@JsonAlias({"studentId"})
	private int studentId;

	@JsonProperty("attempt_id")
	@JsonAlias({"attemptId"})
	private int attemptId;

	@JsonProperty("student_name")
	@JsonAlias({"studentName"})
	private String studentName;

	@JsonProperty("marks_obtained")
	@JsonAlias({"marksObtained", "score"})
	private int marksObtained;

	@JsonProperty("total_marks")
	@JsonAlias({"totalMarks", "total"})
	private int totalMarks;

	@JsonProperty("passing_marks")
	@JsonAlias({"passingMarks"})
	private int passingMarks;

	private double percentage;

	private String status; // "Pass", "Fail", "Submitted for Review", "Disqualified"

	@JsonProperty("violations_count")
	@JsonAlias({"violationsCount"})
	private int violationsCount;

	@JsonProperty("is_disqualified")
	@JsonAlias({"isDisqualified", "disqualified"})
	private boolean disqualified;

	@JsonProperty("disqualification_reason")
	@JsonAlias({"disqualificationReason"})
	private String disqualificationReason;

	@JsonProperty("submitted_at")
	@JsonAlias({"submittedAt"})
	private String submittedAt;

	@JsonProperty("question_breakdown")
	@JsonAlias({"questionBreakdown", "breakdown"})
	private List<Map<String, Object>> questionBreakdown;
}
