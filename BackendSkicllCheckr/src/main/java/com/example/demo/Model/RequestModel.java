package com.example.demo.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestModel {
	private int request_id;
	private String name;
	private String email;
	private String username;
	private String password;
	private String requested_role;
	private String contact;
	private String status;
	@Override
	public String toString() {
		return "RequestModel [request_id=" + request_id + ", name=" + name + ", email=" + email + ", username="
				+ username + ", password=" + password + ", requested_role=" + requested_role + ", contact=" + contact
				+ ", status=" + status + "]";
	}

}
