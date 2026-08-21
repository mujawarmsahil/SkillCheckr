package com.skillcheckr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//import com.skillcheckr.model.User;
import com.skillcheckr.model.User;
import com.skillcheckr.repository.AuthRepository;

@Service
public class AuthServiceImpl  implements AuthService{

	
	@Autowired
	private  AuthRepository authRepository;
	
	@Override
    public User login(String username, String password) {
        return authRepository.login(username, password);
    }

	@Override
	public int getStudentIdByUserId(int userId) {
		// TODO Auto-generated method stub
		return authRepository.getStudentIdByUserId(userId);
	}

	@Override
	public int getTeacherIdByUserId(int userId) {
		// TODO Auto-generated method stub
		return authRepository.getTeacherIdByUserId(userId);
	}

	@Override
	public int getAdminIdByUserId(int userId) {
		// TODO Auto-generated method stub
		return authRepository.getAdminIdByUserId(userId);
	}

}
