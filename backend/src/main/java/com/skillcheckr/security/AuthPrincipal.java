package com.skillcheckr.security;

import java.time.Instant;

/**
 * The authenticated caller resolved from a verified bearer token.
 *
 * <p>{@code userId} is the {@code user.user_id} primary key while {@code roleId} is the
 * primary key of the row in the role specific table (student, teacher or admin) that
 * belongs to the user.</p>
 */
public class AuthPrincipal {

    private final int userId;
    private final int roleId;
    private final String role;
    private final String username;
    private final Instant expiresAt;

    public AuthPrincipal(int userId, int roleId, String role, String username, Instant expiresAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.role = role;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    public int getUserId() {
        return userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isAdmin() {
        return com.skillcheckr.constant.RoleConstants.isAdmin(role);
    }

    public boolean isTeacher() {
        return com.skillcheckr.constant.RoleConstants.isTeacher(role);
    }

    public boolean isStudent() {
        return com.skillcheckr.constant.RoleConstants.isStudent(role);
    }
}
