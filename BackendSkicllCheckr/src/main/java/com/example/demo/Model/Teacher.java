package com.example.demo.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class Teacher {
	private int Teacher_id;
	private int User_id;
	 private String Teacher_name;
	 private String Teacher_contact;
	  private String Teacher_email;
	@Override
	public String toString() {
		return "Teacher [Teacher_id=" + Teacher_id + ", User_id=" + User_id + ", Teacher_name=" + Teacher_name
				+ ", Teacher_contact=" + Teacher_contact + ", Teacher_email=" + Teacher_email + "]";
	}

}
