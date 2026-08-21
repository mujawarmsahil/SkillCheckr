package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {

	@JsonProperty("student_id")
	private int studentId;

	@JsonProperty("student_name")
	private String studentName;

	@JsonProperty("student_contact")
	private String studentContact;

	@JsonProperty("student_email")
	private String studentEmail;
}
