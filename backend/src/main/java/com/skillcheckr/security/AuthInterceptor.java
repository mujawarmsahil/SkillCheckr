package com.skillcheckr.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects unauthenticated requests to protected endpoints and publishes the verified
 * caller identity as a request attribute for the controllers.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE = "skillcheckr.principal";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String POST = "POST";

    /**
     * Public routes are matched on path <em>and</em> HTTP method. A path is only anonymous for
     * the methods listed here, so a public sign up route can never expose the administrative
     * endpoint that happens to share the same path.
     *
     * <p>None of these routes are rate limited: the project ships no rate limiting dependency
     * and none was introduced. The login route is therefore a credential stuffing target and
     * the registration route can create pending rows without limit. Protect these paths at the
     * gateway or add a limiter before relying on this list in a public deployment. See the
     * "Anonymous endpoints and rate limiting" section of architecture.md.
     */
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute("/api/auth/login", POST),
            new PublicRoute("/api/requests", POST),
            new PublicRoute("/actuator/health", null),
            new PublicRoute("/actuator/health/**", null),
            new PublicRoute("/actuator/info", null));

    private record PublicRoute(String path, String method) {
    }

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request)) {
            return true;
        }

        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "Authentication required. Please sign in again.");
            return false;
        }

        Optional<AuthPrincipal> principal = tokenService.verifyToken(token);
        if (principal.isEmpty()) {
            writeUnauthorized(response, "Your session is invalid or has expired. Please sign in again.");
            return false;
        }

        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal.get());
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isEmpty()) {
            return false;
        }
        int contextPathLength = request.getContextPath() == null ? 0 : request.getContextPath().length();
        String normalized = contextPathLength > 0 && path.startsWith(request.getContextPath())
                ? path.substring(contextPathLength)
                : path;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        for (PublicRoute route : PUBLIC_ROUTES) {
            if (matches(route, normalized, request.getMethod())) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(PublicRoute route, String normalizedPath, String method) {
        if (route.path().endsWith("/**")) {
            String prefix = route.path().substring(0, route.path().length() - 2);
            if (!normalizedPath.startsWith(prefix + "/")) {
                return false;
            }
        } else if (!normalizedPath.equalsIgnoreCase(route.path())) {
            return false;
        }
        return route.method() == null || route.method().equalsIgnoreCase(method);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex == null) {
            request.removeAttribute(PRINCIPAL_ATTRIBUTE);
        }
    }
}
