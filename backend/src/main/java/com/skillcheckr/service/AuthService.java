package com.skillcheckr.service;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

public interface AuthService {

    User login(String username, String password);

    int getStudentIdByUserId(int userId);

    int getStudentIdFromAuthorization(String authorizationHeader);

    int getTeacherIdByUserId(int userId);

    int getAdminIdByUserId(int userId);

    UserProfileDTO getUserProfile(int userId);

    UserProfileDTO updateUserProfile(UserProfileDTO profile);

    boolean isUsernameInUse(String username, int excludeUserId);

    boolean isEmailInUse(String email, int excludeUserId);
    boolean verifyCurrentPassword(int userId, String oldPassword);
}
