package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

	@JsonProperty("request_id")
	private int requestId;

	private String name;

	private String email;

	private String username;

	private String password;

	@JsonProperty("requested_role")
	private String requestedRole;

	private String contact;

	private String status;
}
