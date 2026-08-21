package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Teacher {

	@JsonProperty("teacher_id")
	private int teacherId;

	@JsonProperty("user_id")
	private int userId;

	@JsonProperty("teacher_name")
	private String teacherName;

	@JsonProperty("teacher_contact")
	private String teacherContact;

	@JsonProperty("teacher_email")
	private String teacherEmail;
}
