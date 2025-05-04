package com.example.demo.Service;

import com.example.demo.Model.*;
public interface AuthService {

	
	 User login(String username, String password);
	 
	 int getStudentIdByUserId(int userId);
	    int getTeacherIdByUserId(int userId);
	    int getAdminIdByUserId(int userId);
}
