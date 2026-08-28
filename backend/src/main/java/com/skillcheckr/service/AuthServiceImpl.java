package com.skillcheckr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.repository.AuthRepository;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Override
    public User login(String username, String password) {
        return authRepository.login(username, password);
    }

    @Override
    public int getStudentIdByUserId(int userId) {
        return authRepository.getStudentIdByUserId(userId);
    }

    @Override
    public int getTeacherIdByUserId(int userId) {
        return authRepository.getTeacherIdByUserId(userId);
    }

    @Override
    public int getAdminIdByUserId(int userId) {
        return authRepository.getAdminIdByUserId(userId);
    }

    @Override
    public UserProfileDTO getUserProfile(int userId) {
        return authRepository.getUserProfile(userId);
    }

    @Override
    public UserProfileDTO updateUserProfile(UserProfileDTO profile) {
        return authRepository.updateUserProfile(profile);
    }

    @Override
    public boolean isUsernameInUse(String username, int excludeUserId) {
        return authRepository.isUsernameInUse(username, excludeUserId);
    }

    @Override
    public boolean isEmailInUse(String email, int excludeUserId) {
        return authRepository.isEmailInUse(email, excludeUserId);
    }
}
