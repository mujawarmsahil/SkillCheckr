package com.skillcheckr.repository;

import java.util.Optional;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

public interface AuthRepository {

    Optional<User> findByUsername(String username);

    boolean updatePassword(int userId, String passwordHash);

    Optional<String> findPasswordByUserId(int userId);

    int getStudentIdByUserId(int userId);

    int getTeacherIdByUserId(int userId);

    int getAdminIdByUserId(int userId);

    Optional<UserProfileDTO> getUserProfile(int userId);

    Optional<UserProfileDTO> updateUserProfile(UserProfileDTO profile);

    boolean isUsernameInUse(String username, int excludeUserId);

    boolean existsByUsername(String username);

    boolean isEmailInUse(String email, int excludeUserId);
}
