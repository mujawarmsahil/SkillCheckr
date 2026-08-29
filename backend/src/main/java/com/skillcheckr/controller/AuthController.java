package com.skillcheckr.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.LoginRequest;
import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.service.AuthService;

@RestController
@RequestMapping({"/api/authentication", "/api/auth", "/api/user", "/api/users"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Username and password are required"));
        }

        try {
            User user = authService.login(request.getUsername().trim(), request.getPassword());
            if (user != null) {
                UserProfileDTO fullProfile = null;
                try {
                    fullProfile = authService.getUserProfile(user.getUserId());
                } catch (Exception ignored) {
                }

                int roleId = 0;
                if (fullProfile != null && fullProfile.getRoleId() > 0) {
                    roleId = fullProfile.getRoleId();
                } else if ("Student".equalsIgnoreCase(user.getRole())) {
                    try { roleId = authService.getStudentIdByUserId(user.getUserId()); } catch (Exception ignored) {}
                } else if ("Teacher".equalsIgnoreCase(user.getRole())) {
                    try { roleId = authService.getTeacherIdByUserId(user.getUserId()); } catch (Exception ignored) {}
                } else if ("Admin".equalsIgnoreCase(user.getRole())) {
                    try { roleId = authService.getAdminIdByUserId(user.getUserId()); } catch (Exception ignored) {}
                }

                String name = fullProfile != null && fullProfile.getName() != null ? fullProfile.getName() : user.getUsername();
                String email = fullProfile != null && fullProfile.getEmail() != null ? fullProfile.getEmail() : "";
                String contact = fullProfile != null && fullProfile.getContact() != null ? fullProfile.getContact() : "";
                String profileImage = fullProfile != null && fullProfile.getProfileImage() != null ? fullProfile.getProfileImage() : user.getProfileImage();

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login Successful");
                response.put("username", user.getUsername());
                response.put("name", name);
                response.put("email", email);
                response.put("contact", contact);
                if (profileImage != null) {
                    response.put("profile_image", profileImage);
                    response.put("profileImage", profileImage);
                }
                response.put("role", user.getRole());
                response.put("userId", user.getUserId());
                response.put("roleId", roleId);
                response.put("user_id", user.getUserId());
                response.put("role_id", roleId);
                response.put("token", "jwt-mock-" + user.getUserId() + "-" + System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid username or password"));
            }
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Login error: " + e.getMessage()));
        }
    }

    @GetMapping({"/profile/{userId}", "/user/profile/{userId}"})
    public ResponseEntity<?> getUserProfile(@PathVariable("userId") Integer userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Valid User ID is required"));
        }

        try {
            UserProfileDTO profile = authService.getUserProfile(userId);
            if (profile != null) {
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching user profile: " + e.getMessage()));
        }
    }

    @PutMapping({"/profile/{userId}", "/user/profile/{userId}"})
    public ResponseEntity<?> updateProfile(@PathVariable("userId") Integer userId, @RequestBody UserProfileDTO profileRequest) {
        if (userId == null || userId <= 0 || profileRequest == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid profile update data"));
        }
        profileRequest.setUserId(userId);
        return handleProfileUpdate(profileRequest);
    }

    @PostMapping({"/profile/update", "/profile"})
    public ResponseEntity<?> updateProfilePost(@RequestBody UserProfileDTO profileRequest) {
        if (profileRequest == null || profileRequest.getUserId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Valid User ID is required in profile data"));
        }
        return handleProfileUpdate(profileRequest);
    }

    private ResponseEntity<?> handleProfileUpdate(UserProfileDTO profile) {
        // Validate required fields
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        if (profile.getUsername() == null || profile.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        if (profile.getEmail() == null || profile.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        if (!profile.getEmail().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please provide a valid email address"));
        }

        String trimmedUsername = profile.getUsername().trim();
        String trimmedEmail = profile.getEmail().trim();
        profile.setUsername(trimmedUsername);
        profile.setName(profile.getName().trim());
        profile.setEmail(trimmedEmail);
        if (profile.getContact() != null) {
            profile.setContact(profile.getContact().trim());
        }

        // Check duplicate username
        if (authService.isUsernameInUse(trimmedUsername, profile.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken by another account"));
        }

        // Check duplicate email
        if (authService.isEmailInUse(trimmedEmail, profile.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use by another account"));
        }

        // Validate password if provided
        if (profile.getPassword() != null && !profile.getPassword().trim().isEmpty()) {
            String newPassword = profile.getPassword().trim();
            if (newPassword.length() < 4) {
                return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 4 characters long"));
            }

            if (profile.getOldPassword() == null || profile.getOldPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Current password is required to change password"));
            }

            boolean isOldPasswordValid = authService.verifyCurrentPassword(profile.getUserId(), profile.getOldPassword().trim());
            if (!isOldPasswordValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid current password. Cannot change password."));
            }

            profile.setPassword(newPassword);
        } else {
            profile.setPassword(null); // Keep existing password
        }

        try {
            UserProfileDTO updated = authService.updateUserProfile(profile);
            if (updated != null) {
                return ResponseEntity.ok(Map.of(
                        "message", "Profile updated successfully",
                        "success", true,
                        "profile", updated
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found or update failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating profile: " + e.getMessage()));
        }
    }
}

