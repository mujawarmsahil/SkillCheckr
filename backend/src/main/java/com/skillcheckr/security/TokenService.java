package com.skillcheckr.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.skillcheckr.constant.RoleConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Issues and verifies the compact bearer tokens used by the API.
 *
 * <p>Tokens are HMAC-SHA256 signed and therefore unforgeable without the server secret.
 * The wire format is {@code sc1.<base64url(payload)>.<base64url(signature)>} where the
 * decoded payload is {@code userId:roleId:role:issuedAtEpochSecond:expiresAtEpochSecond:username}.
 * Verification is constant time, checks the expiry, and rejects any token whose role is
 * unknown so a role can never be escalated by tampering with the payload.</p>
 */
@Slf4j
@Component
public class TokenService {

    static final String TOKEN_PREFIX = "sc1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int PAYLOAD_FIELDS = 6;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;

    public TokenService(@Value("${app.security.token-secret}") String tokenSecret,
            @Value("${app.security.token-ttl-minutes:120}") long tokenTtlMinutes) {
        String resolved = (tokenSecret == null || tokenSecret.isBlank()) ? "" : tokenSecret.trim();
        if (resolved.length() < 32) {
            log.warn("app.security.token-secret is shorter than 32 characters or unset; "
                    + "set the TOKEN_SECRET environment variable in every deployed environment.");
        }
        if (resolved.isEmpty()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.secret = random;
            log.warn("No app.security.token-secret configured; generated an ephemeral secret. "
                    + "All issued tokens become invalid on restart.");
        } else {
            this.secret = resolved.getBytes(StandardCharsets.UTF_8);
        }
        this.ttl = Duration.ofMinutes(Math.max(1, tokenTtlMinutes));
    }

    public String issueToken(int userId, int roleId, String role, String username) {
        Instant issuedAt = Instant.now();
        return issueToken(new AuthPrincipal(userId, roleId, role, username,
                issuedAt.plus(ttl)));
    }

    public String issueToken(AuthPrincipal principal) {
        String payload = String.join(":",
                Integer.toString(principal.getUserId()),
                Integer.toString(principal.getRoleId()),
                principal.getRole() == null ? "" : principal.getRole(),
                Long.toString(principal.getExpiresAt().minus(ttl).getEpochSecond()),
                Long.toString(principal.getExpiresAt().getEpochSecond()),
                principal.getUsername() == null ? "" : principal.getUsername());
        String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        // The signature has to be base64url encoded as well, otherwise the raw bytes end up
        // in the token string as "[B@..." and the token can never be parsed back.
        String encodedSignature = ENCODER.encodeToString(sign(encodedPayload));
        return TOKEN_PREFIX + "." + encodedPayload + "." + encodedSignature;
    }

    /**
     * Verifies the signature, the expiry and the role of a token.
     *
     * @return the caller identity, or empty when the token is missing, malformed,
     *         tampered with, expired or carries an unknown role
     */
    public Optional<AuthPrincipal> verifyToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String value = token.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        if (value.isEmpty()) {
            return Optional.empty();
        }

        String[] segments = value.split("\\.");
        if (segments.length != 3 || !TOKEN_PREFIX.equals(segments[0])) {
            return Optional.empty();
        }

        byte[] providedSignature;
        String decodedPayload;
        try {
            providedSignature = DECODER.decode(segments[2]);
            decodedPayload = new String(DECODER.decode(segments[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        byte[] expectedSignature = sign(segments[1]);
        if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
            return Optional.empty();
        }

        String[] fields = decodedPayload.split(":", PAYLOAD_FIELDS);
        if (fields.length != PAYLOAD_FIELDS) {
            return Optional.empty();
        }

        try {
            int userId = Integer.parseInt(fields[0]);
            int roleId = Integer.parseInt(fields[1]);
            long issuedAt = Long.parseLong(fields[3]);
            long expiresAt = Long.parseLong(fields[4]);
            String role = fields[2];
            String username = fields[5];

            if (userId <= 0 || expiresAt <= 0 || issuedAt <= 0) {
                return Optional.empty();
            }
            if (!RoleConstants.isAdmin(role) && !RoleConstants.isTeacher(role) && !RoleConstants.isStudent(role)) {
                return Optional.empty();
            }
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }
            if (roleId <= 0) {
                return Optional.empty();
            }

            return Optional.of(new AuthPrincipal(userId, roleId, role, username,
                    Instant.ofEpochSecond(expiresAt)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to sign authentication token", ex);
        }
    }
}
