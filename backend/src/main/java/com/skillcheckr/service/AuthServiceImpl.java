package com.skillcheckr.service;

import java.util.Optional;

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
    public Optional<User> login(String username, String password) {
        return authRepository.login(username, password);
    }

    @Override
    public int getStudentIdByUserId(int userId) {
        return authRepository.getStudentIdByUserId(userId);
    }

    @Override
    public int getStudentIdFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return 0;
        }

        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("-");
        if (parts.length < 3) {
            return 0;
        }

        try {
            int userId = Integer.parseInt(parts[parts.length - 2]);
            return getStudentIdByUserId(userId);
        } catch (NumberFormatException ex) {
            return 0;
        }
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
    public Optional<UserProfileDTO> getUserProfile(int userId) {
        return authRepository.getUserProfile(userId);
    }

    @Override
    public Optional<UserProfileDTO> updateUserProfile(UserProfileDTO profile) {
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

    @Override
    public boolean verifyCurrentPassword(int userId, String oldPassword) {
        return authRepository.verifyCurrentPassword(userId, oldPassword);
    }
}
