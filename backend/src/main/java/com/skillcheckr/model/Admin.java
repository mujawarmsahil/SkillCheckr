package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Admin {
	
	
	@JsonProperty("admin_id")
	private int adminId;
	@JsonProperty("user_id")
	private int userId;
	@JsonProperty("admin_name")
	private String adminName;
	@JsonProperty("admin_email")
	private String adminEmail;
	@JsonProperty("admin_contact")
	private String adminContact;
	@JsonProperty("profile_image")
	private String profileImage;
	 
	 
	
	

}
