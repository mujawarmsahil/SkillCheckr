package com.skillcheckr.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.skillcheckr.service.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AuthService authService;

    public TokenAuthenticationFilter(TokenService tokenService, AuthService authService) {
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(AuthInterceptor.AUTHORIZATION_HEADER);
        if (header != null && !header.isBlank()) {
            var principal = tokenService.verifyToken(header);
            if (principal.isPresent() && authService.isUserActive(principal.get().getUserId())) {
                AuthPrincipal p = principal.get();
                var auth = new UsernamePasswordAuthenticationToken(p, null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + p.getRole())));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(AuthInterceptor.PRINCIPAL_ATTRIBUTE, p);
            }
        }
        filterChain.doFilter(request, response);
    }
}
