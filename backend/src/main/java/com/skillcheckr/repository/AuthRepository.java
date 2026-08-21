package com.skillcheckr.repository;

import com.skillcheckr.model.User;

public interface AuthRepository {

	
	    User login(String username, String password);
	    int getStudentIdByUserId(int userId);
	    int getTeacherIdByUserId(int userId);
	    int getAdminIdByUserId(int userId);

}
