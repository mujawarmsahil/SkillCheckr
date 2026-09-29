package com.skillcheckr.constant;

/**
 * Canonical role names as persisted in {@code user.user_role}.
 */
public final class RoleConstants {

    private RoleConstants() {
    }

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_TEACHER = "Teacher";
    public static final String ROLE_STUDENT = "Student";

    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isTeacher(String role) {
        return ROLE_TEACHER.equalsIgnoreCase(role);
    }

    public static boolean isStudent(String role) {
        return ROLE_STUDENT.equalsIgnoreCase(role);
    }
}
