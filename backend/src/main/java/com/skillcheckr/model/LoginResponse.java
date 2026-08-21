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
public class LoginResponse {

	private String message;
	private String username;
	private String role;

	@JsonProperty("user_id")
	@JsonAlias({"userId"})
	private int userId;

	@JsonProperty("role_id")
	@JsonAlias({"roleId"})
	private int roleId;

	private String token;
}
