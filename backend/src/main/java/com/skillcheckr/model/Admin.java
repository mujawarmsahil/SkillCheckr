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
public class Admin {

	@JsonProperty("admin_id")
	@JsonAlias({"admin_id", "adminId", "id"})
	private int adminId;

	@JsonProperty("user_id")
	@JsonAlias({"user_id", "userId"})
	private int userId;

	@JsonProperty("name")
	@JsonAlias({"name", "admin_name", "adminName"})
	private String name;

	@JsonProperty("contact")
	@JsonAlias({"contact", "admin_contact", "adminContact"})
	private String contact;

	@JsonProperty("email")
	@JsonAlias({"email", "admin_email", "adminEmail"})
	private String email;

	@JsonProperty("profile_image")
	@JsonAlias({"profile_image", "profileImage"})
	private String profileImage;

	// Backward compatibility accessors
	public String getAdminName() {
		return name;
	}

	public void setAdminName(String adminName) {
		this.name = adminName;
	}

	public String getAdminContact() {
		return contact;
	}

	public void setAdminContact(String adminContact) {
		this.contact = adminContact;
	}

	public String getAdminEmail() {
		return email;
	}

	public void setAdminEmail(String adminEmail) {
		this.email = adminEmail;
	}
}
