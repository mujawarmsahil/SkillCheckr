package com.example.demo.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
private int Student_id; 
private String Student_name;
private String Student_Contact;
private String Student_email;
@Override
public String toString() {
	return "Student [Student_id=" + Student_id + ", Student_name=" + Student_name + ", Student_Contact="
			+ Student_Contact + ", Student_email=" + Student_email + "]";
}


}
