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
public class Student {

	@JsonProperty("student_id")
	@JsonAlias({"student_id", "studentId", "id"})
	private int studentId;

	@JsonProperty("user_id")
	@JsonAlias({"user_id", "userId"})
	private int userId;

	@JsonProperty("name")
	@JsonAlias({"name", "student_name", "studentName"})
	private String name;

	@JsonProperty("contact")
	@JsonAlias({"contact", "student_contact", "studentContact"})
	private String contact;

	@JsonProperty("email")
	@JsonAlias({"email", "student_email", "studentEmail"})
	private String email;

	@JsonProperty("profile_image")
	@JsonAlias({"profile_image", "profileImage"})
	private String profileImage;

	// Backward compatibility accessors
	public String getStudentName() {
		return name;
	}

	public void setStudentName(String studentName) {
		this.name = studentName;
	}

	public String getStudentContact() {
		return contact;
	}

	public void setStudentContact(String studentContact) {
		this.contact = studentContact;
	}

	public String getStudentEmail() {
		return email;
	}

	public void setStudentEmail(String studentEmail) {
		this.email = studentEmail;
	}
}
