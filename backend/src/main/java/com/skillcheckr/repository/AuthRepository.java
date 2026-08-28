package com.skillcheckr.repository;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

public interface AuthRepository {

    User login(String username, String password);

    int getStudentIdByUserId(int userId);

    int getTeacherIdByUserId(int userId);

    int getAdminIdByUserId(int userId);

    UserProfileDTO getUserProfile(int userId);

    UserProfileDTO updateUserProfile(UserProfileDTO profile);

    boolean isUsernameInUse(String username, int excludeUserId);

    boolean isEmailInUse(String email, int excludeUserId);
}
