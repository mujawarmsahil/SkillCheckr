package com.skillcheckr.support;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.skillcheckr.constant.RoleConstants;
import com.skillcheckr.security.PublicEndpointRateLimiter;
import com.skillcheckr.security.AuthInterceptor;
import com.skillcheckr.security.TokenService;
import com.skillcheckr.service.AuthService;

/**
 * Builds the bearer tokens used by the controller tests. The real {@link TokenService} and
 * {@link AuthInterceptor} are wired into the standalone MockMvc instances so authentication
 * and role checks are exercised by the tests instead of being bypassed.
 */
public final class TestAuth {

    private static final String TEST_SECRET = "skillcheckr-test-secret-key-value-for-unit-tests";

    private TestAuth() {
    }

    public static TokenService tokenService() {
        return new TokenService(TEST_SECRET, 60);
    }

    public static AuthInterceptor authInterceptor() {
        AuthService authService = mock(AuthService.class);
        lenient().when(authService.isUserActive(anyInt())).thenReturn(true);
        return authInterceptor(authService);
    }

    public static AuthInterceptor authInterceptor(AuthService authService) {
        return new AuthInterceptor(tokenService(), authService,
                new PublicEndpointRateLimiter(10, 60, 5, 3600));
    }

    public static String tokenFor(int userId, int roleId, String role) {
        return tokenService().issueToken(userId, roleId, role, "test-" + role.toLowerCase());
    }

    public static RequestPostProcessor asStudent(int studentId) {
        return as(RoleConstants.ROLE_STUDENT, studentId);
    }

    public static RequestPostProcessor asTeacher(int teacherId) {
        return as(RoleConstants.ROLE_TEACHER, teacherId);
    }

    public static RequestPostProcessor asAdmin(int adminId) {
        return as(RoleConstants.ROLE_ADMIN, adminId);
    }

    public static RequestPostProcessor as(String role, int roleId) {
        return asUser(roleId, role, roleId);
    }

    /**
     * Builds a token where the account id and the role table row id differ, which is the normal
     * situation in the database and the only way to catch an ownership check that wrongly
     * compares a {@code user.user_id} with a role id.
     */
    public static RequestPostProcessor asUser(int userId, String role, int roleId) {
        return request -> {
            request.addHeader(AuthInterceptor.AUTHORIZATION_HEADER,
                    "Bearer " + tokenFor(userId, roleId, role));
            return request;
        };
    }

    public static RequestPostProcessor asStudentAccount(int userId, int studentId) {
        return asUser(userId, RoleConstants.ROLE_STUDENT, studentId);
    }

    public static RequestPostProcessor asTeacherAccount(int userId, int teacherId) {
        return asUser(userId, RoleConstants.ROLE_TEACHER, teacherId);
    }

    public static RequestPostProcessor asAdminAccount(int userId, int adminId) {
        return asUser(userId, RoleConstants.ROLE_ADMIN, adminId);
    }

    /**
     * A token that carries a valid signature but an unknown role, which the token service
     * must reject instead of trusting.
     */
    public static RequestPostProcessor withRole(String role) {
        return request -> {
            request.addHeader(AuthInterceptor.AUTHORIZATION_HEADER, "Bearer " + tokenFor(9, 9, role));
            return request;
        };
    }

    public static RequestPostProcessor withToken(String token) {
        return request -> {
            request.addHeader(AuthInterceptor.AUTHORIZATION_HEADER, token);
            return request;
        };
    }
}
