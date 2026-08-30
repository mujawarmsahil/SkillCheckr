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
public class Teacher {

	@JsonProperty("teacher_id")
	@JsonAlias({"teacher_id", "teacherId", "id"})
	private int teacherId;

	@JsonProperty("user_id")
	@JsonAlias({"user_id", "userId"})
	private int userId;

	@JsonProperty("name")
	@JsonAlias({"name", "teacher_name", "teacherName"})
	private String name;

	@JsonProperty("contact")
	@JsonAlias({"contact", "teacher_contact", "teacherContact"})
	private String contact;

	@JsonProperty("email")
	@JsonAlias({"email", "teacher_email", "teacherEmail"})
	private String email;

	@JsonProperty("profile_image")
	@JsonAlias({"profile_image", "profileImage"})
	private String profileImage;

	@JsonProperty("status")
	@JsonAlias({"status", "is_active", "isActive"})
	private String status;

	// Backward compatibility accessors
	@JsonProperty("teacher_name")
	public String getTeacherName() {
		return name;
	}

	public void setTeacherName(String teacherName) {
		this.name = teacherName;
	}

	@JsonProperty("teacher_contact")
	public String getTeacherContact() {
		return contact;
	}

	public void setTeacherContact(String teacherContact) {
		this.contact = teacherContact;
	}

	@JsonProperty("teacher_email")
	public String getTeacherEmail() {
		return email;
	}

	public void setTeacherEmail(String teacherEmail) {
		this.email = teacherEmail;
	}
}
