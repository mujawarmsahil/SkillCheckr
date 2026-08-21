package com.skillcheckr.service;

import com.skillcheckr.model.*;
public interface AuthService {

	
	 User login(String username, String password);
	 
	 int getStudentIdByUserId(int userId);
	    int getTeacherIdByUserId(int userId);
	    int getAdminIdByUserId(int userId);
}
