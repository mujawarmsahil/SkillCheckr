package com.skillcheckr.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.exception.AccountDisabledException;
import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.repository.AuthRepository;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Verifies the credentials and then refuses a deactivated account. The check lives here rather
     * than in the controller so that every caller of the service is covered, and so the
     * "wrong password" answer cannot accidentally be given to a user whose account was switched off
     * by an administrator.
     *
     * <p>A stored value that is not a BCrypt hash is treated as legacy plaintext and is upgraded
     * to BCrypt on the next successful sign in. The comparison is done in constant
     * time so it does not leak the matching prefix length through response timing.
     *
     * @return the authenticated user, never empty for a disabled account: that case throws
     * @throws AccountDisabledException when the password is correct but the account is inactive
     */
    @Override
    @Transactional
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

        boolean matches = false;
        if (storedPassword != null) {
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                    || storedPassword.startsWith("$2y$")) {
                matches = passwordEncoder.matches(password, storedPassword);
            } else {
                matches = MessageDigest.isEqual(
                        storedPassword.getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8));
                if (matches) {
                    try {
                        String newHash = passwordEncoder.encode(password);
                        authRepository.updatePassword(user.getUserId(), newHash);
                        user.setPassword(newHash);
                    } catch (Exception ignored) {
                        // Non-critical auto-upgrade failure
                    }
                }
            }
        }

        if (!matches) {
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
    public boolean isUsernameInUse(String username, int excludeUserId) {
        return authRepository.isUsernameInUse(username, excludeUserId);
    }

    @Override
    public boolean isEmailInUse(String email, int excludeUserId) {
        return authRepository.isEmailInUse(email, excludeUserId);
    }

    @Override
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
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(oldPassword, storedPassword);
        } else {
            return storedPassword.equals(oldPassword);
        }
    }
}
