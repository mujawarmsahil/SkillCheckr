package com.skillcheckr.model;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Exam {

	@JsonProperty("exam_id")
	@JsonAlias({"examId", "id"})
	private int examId;

	@JsonProperty("teacher_id")
	@JsonAlias({"teacherId"})
	private int teacherId;

	private Subject subject;

	@JsonProperty("exam_name")
	@JsonAlias({"examName"})
	private String examName;

	@JsonProperty("exam_type")
	@JsonAlias({"examType"})
	private String examType; // "MCQ" or "QUESTION_ANSWER" (Descriptive)

	private String date;

	@JsonProperty("start_time")
	@JsonAlias({"startTime"})
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime startTime;

	@JsonProperty("end_time")
	@JsonAlias({"endTime"})
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime endTime;

	@JsonProperty("duration_minutes")
	@JsonAlias({"duration_minuets", "durationMinutes"})
	private int durationMinutes;

	@JsonProperty("total_marks")
	@JsonAlias({"totalMarks"})
	private int totalMarks;

	@JsonProperty("passing_marks")
	@JsonAlias({"passingMarks", "pass_marks"})
	private int passingMarks;

	private String status;
}
