package com.skillcheckr.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.exception.AccountDisabledException;
import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.repository.AuthRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Verifies BCrypt credentials and rejects accounts that have been deactivated.
     *
     * @throws AccountDisabledException when the credentials are valid but the account is inactive
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = authRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();

        if (storedPassword == null || !passwordEncoder.matches(password, storedPassword)) {
            return Optional.empty();
        }

        if (!isActive(user.getStatus())) {
            throw new AccountDisabledException(
                    "This account has been deactivated. Please contact your administrator.");
        }

        return Optional.of(user);
    }

    /**
     * A missing status is treated as active so that an account is never locked out by data that
     * predates the status column.
     */
    private boolean isActive(String status) {
        return status == null || status.trim().isEmpty() || !"Inactive".equalsIgnoreCase(status.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public int getStudentIdByUserId(int userId) {
        return authRepository.getStudentIdByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTeacherIdByUserId(int userId) {
        return authRepository.getTeacherIdByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAdminIdByUserId(int userId) {
        return authRepository.getAdminIdByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getUserProfile(int userId) {
        return authRepository.getUserProfile(userId);
    }

    @Override
    @Transactional
    public Optional<UserProfileDTO> updateUserProfile(UserProfileDTO profile) {
        if (profile != null && profile.getPassword() != null && !profile.getPassword().trim().isEmpty()) {
            profile.setPassword(passwordEncoder.encode(profile.getPassword().trim()));
        }
        return authRepository.updateUserProfile(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameInUse(String username, int excludeUserId) {
        return authRepository.isUsernameInUse(username, excludeUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailInUse(String email, int excludeUserId) {
        return authRepository.isEmailInUse(email, excludeUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserActive(int userId) {
        return authRepository.isUserActive(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyCurrentPassword(int userId, String oldPassword) {
        if (oldPassword == null || oldPassword.isEmpty() || userId <= 0) {
            return false;
        }
        Optional<String> storedPasswordOpt = authRepository.findPasswordByUserId(userId);
        if (storedPasswordOpt.isEmpty()) {
            return false;
        }
        String storedPassword = storedPasswordOpt.get();
        if (storedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(oldPassword, storedPassword);
    }
}
