package com.skillcheckr.security;

import java.util.Arrays;

import com.skillcheckr.constant.RoleConstants;
import com.skillcheckr.exception.ForbiddenException;
import com.skillcheckr.exception.UnauthorizedException;
import com.skillcheckr.model.Exam;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller side authorization helpers. Every protected endpoint resolves the caller
 * through this class so role and ownership rules stay in one place instead of being
 * re-implemented per controller.
 */
public final class AuthGuard {

    private AuthGuard() {
    }

    public static AuthPrincipal requirePrincipal(HttpServletRequest request) {
        Object attribute = request == null ? null : request.getAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE);
        if (!(attribute instanceof AuthPrincipal principal)) {
            throw new UnauthorizedException("Authentication required. Please sign in again.");
        }
        return principal;
    }

    public static AuthPrincipal requireRole(HttpServletRequest request, String... allowedRoles) {
        AuthPrincipal principal = requirePrincipal(request);
        boolean allowed = Arrays.stream(allowedRoles)
                .anyMatch(role -> role.equalsIgnoreCase(principal.getRole()));
        if (!allowed) {
            throw new ForbiddenException("You are not authorized to perform this action.");
        }
        return principal;
    }

    public static AuthPrincipal requireAdmin(HttpServletRequest request) {
        return requireRole(request, RoleConstants.ROLE_ADMIN);
    }

    public static AuthPrincipal requireStaff(HttpServletRequest request) {
        return requireRole(request, RoleConstants.ROLE_ADMIN, RoleConstants.ROLE_TEACHER);
    }

    public static AuthPrincipal requireStudent(HttpServletRequest request) {
        return requireRole(request, RoleConstants.ROLE_STUDENT);
    }

    public static int requireStudentId(HttpServletRequest request) {
        AuthPrincipal principal = requireStudent(request);
        if (principal.getRoleId() <= 0) {
            throw new ForbiddenException("No student profile is linked to this account.");
        }
        return principal.getRoleId();
    }

    public static AuthPrincipal requireSelfOrStaff(HttpServletRequest request, int ownerId) {
        AuthPrincipal principal = requirePrincipal(request);
        if (principal.isAdmin() || principal.isTeacher()) {
            return principal;
        }
        if (principal.getRoleId() != ownerId) {
            throw new ForbiddenException("You are not authorized to access another user's data.");
        }
        return principal;
    }

    /**
     * Account level ownership: the owner identifier is a {@code user.user_id}, so it must be
     * compared with the authenticated account id and never with the role table id.
     */
    public static AuthPrincipal requireSelfOrAdmin(HttpServletRequest request, int ownerId) {
        AuthPrincipal principal = requirePrincipal(request);
        if (principal.isAdmin() || principal.getUserId() == ownerId) {
            return principal;
        }
        throw new ForbiddenException("You are not authorized to access this resource.");
    }

    /**
     * Role table ownership: the owner identifier is a {@code student.student_id} or
     * {@code teacher.teacher_id}, so it must be compared with the authenticated role id.
     * Staff are deliberately not granted blanket access here, because a list scoped only by
     * that id would expose every record of that role regardless of which exam it belongs to.
     */
    public static AuthPrincipal requireSelfOrAdminForRoleData(HttpServletRequest request, int ownerRoleId) {
        AuthPrincipal principal = requirePrincipal(request);
        if (principal.isAdmin()
                || (principal.isStudent() && principal.getRoleId() == ownerRoleId)) {
            return principal;
        }
        throw new ForbiddenException("You are not authorized to access another user's data.");
    }

    public static AuthPrincipal requireExamAccess(HttpServletRequest request, Exam exam) {
        AuthPrincipal principal = requirePrincipal(request);
        if (principal.isAdmin()) {
            return principal;
        }
        if (principal.isTeacher() && exam != null && exam.getTeacherId() == principal.getRoleId()) {
            return principal;
        }
        throw new ForbiddenException("You are not authorized to manage this exam.");
    }
}
