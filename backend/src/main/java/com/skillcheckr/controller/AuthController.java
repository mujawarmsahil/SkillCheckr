package com.skillcheckr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.exception.UnauthorizedException;
import com.skillcheckr.model.LoginRequest;
import com.skillcheckr.model.LoginResponse;
import com.skillcheckr.model.ProfileUpdateResponse;
import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.service.AuthService;

@RestController
@RequestMapping({"/api/authentication", "/api/auth", "/api/user", "/api/users"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Username and password are required");
        }

        User user = authService.login(request.getUsername().trim(), request.getPassword())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        UserProfileDTO fullProfile = authService.getUserProfile(user.getUserId()).orElse(null);

        int roleId = 0;
        if (fullProfile != null && fullProfile.getRoleId() > 0) {
            roleId = fullProfile.getRoleId();
        } else if ("Student".equalsIgnoreCase(user.getRole())) {
            roleId = authService.getStudentIdByUserId(user.getUserId());
        } else if ("Teacher".equalsIgnoreCase(user.getRole())) {
            roleId = authService.getTeacherIdByUserId(user.getUserId());
        } else if ("Admin".equalsIgnoreCase(user.getRole())) {
            roleId = authService.getAdminIdByUserId(user.getUserId());
        }

        String name = fullProfile != null && fullProfile.getName() != null ? fullProfile.getName() : user.getUsername();
        String email = fullProfile != null && fullProfile.getEmail() != null ? fullProfile.getEmail() : "";
        String contact = fullProfile != null && fullProfile.getContact() != null ? fullProfile.getContact() : "";
        String profileImage = fullProfile != null && fullProfile.getProfileImage() != null
                ? fullProfile.getProfileImage()
                : user.getProfileImage();

        return ResponseEntity.ok(LoginResponse.builder()
                .message("Login Successful")
                .username(user.getUsername())
                .name(name)
                .email(email)
                .contact(contact)
                .profileImage(profileImage)
                .role(user.getRole())
                .userId(user.getUserId())
                .roleId(roleId)
                .token("jwt-mock-" + user.getUserId() + "-" + System.currentTimeMillis())
                .build());
    }

    @GetMapping({"/profile/{userId}", "/user/profile/{userId}"})
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable("userId") Integer userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Valid User ID is required");
        }

        UserProfileDTO profile = authService.getUserProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(profile);
    }

    @PutMapping({"/profile/{userId}", "/user/profile/{userId}"})
    public ResponseEntity<ProfileUpdateResponse> updateProfile(@PathVariable("userId") Integer userId,
            @RequestBody UserProfileDTO profileRequest) {
        if (userId == null || userId <= 0 || profileRequest == null) {
            throw new BadRequestException("Invalid profile update data");
        }
        profileRequest.setUserId(userId);
        return ResponseEntity.ok(performProfileUpdate(profileRequest));
    }

    @PostMapping({"/profile/update", "/profile"})
    public ResponseEntity<ProfileUpdateResponse> updateProfilePost(@RequestBody UserProfileDTO profileRequest) {
        if (profileRequest == null || profileRequest.getUserId() <= 0) {
            throw new BadRequestException("Valid User ID is required in profile data");
        }
        return ResponseEntity.ok(performProfileUpdate(profileRequest));
    }

    private ProfileUpdateResponse performProfileUpdate(UserProfileDTO profile) {
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            throw new BadRequestException("Name is required");
        }
        if (profile.getUsername() == null || profile.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required");
        }
        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (!profile.getEmail().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BadRequestException("Please provide a valid email address");
        }

        String trimmedUsername = profile.getUsername().trim();
        String trimmedEmail = profile.getEmail().trim();
        profile.setUsername(trimmedUsername);
        profile.setName(profile.getName().trim());
        profile.setEmail(trimmedEmail);
        if (profile.getContact() != null) {
            profile.setContact(profile.getContact().trim());
        }

        if (authService.isUsernameInUse(trimmedUsername, profile.getUserId())) {
            throw new BadRequestException("Username is already taken by another account");
        }

        if (authService.isEmailInUse(trimmedEmail, profile.getUserId())) {
            throw new BadRequestException("Email is already in use by another account");
        }

        if (profile.getPassword() != null && !profile.getPassword().trim().isEmpty()) {
            String newPassword = profile.getPassword().trim();
            if (newPassword.length() < 4) {
                throw new BadRequestException("New password must be at least 4 characters long");
            }

            if (profile.getOldPassword() == null || profile.getOldPassword().trim().isEmpty()) {
                throw new BadRequestException("Current password is required to change password");
            }

            boolean isOldPasswordValid = authService.verifyCurrentPassword(profile.getUserId(), profile.getOldPassword().trim());
            if (!isOldPasswordValid) {
                throw new BadRequestException("Invalid current password. Cannot change password.");
            }

            profile.setPassword(newPassword);
        } else {
            profile.setPassword(null); // Keep existing password
        }

        UserProfileDTO updated = authService.updateUserProfile(profile)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or update failed"));

        return ProfileUpdateResponse.builder()
                .message("Profile updated.")
                .success(true)
                .profile(updated)
                .build();
    }
}