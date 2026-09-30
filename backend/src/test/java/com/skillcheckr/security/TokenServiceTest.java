package com.skillcheckr.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.skillcheckr.constant.RoleConstants;

class TokenServiceTest {

    private static final String SECRET = "skillcheckr-unit-test-secret-value-long-enough";
    private static final String OTHER_SECRET = "a-completely-different-secret-value-here";

    private TokenService tokenService() {
        return new TokenService(SECRET, 60);
    }

    @Test
    void issuesAVerifiableTokenWithTheCallerIdentity() {
        String token = tokenService().issueToken(12, 34, RoleConstants.ROLE_TEACHER, "rita");

        assertThat(token).startsWith("sc1.").doesNotContain("[B@");

        Optional<AuthPrincipal> verified = tokenService().verifyToken("Bearer " + token);

        assertThat(verified).isPresent();
        assertThat(verified.get().getUserId()).isEqualTo(12);
        assertThat(verified.get().getRoleId()).isEqualTo(34);
        assertThat(verified.get().getRole()).isEqualTo(RoleConstants.ROLE_TEACHER);
        assertThat(verified.get().getUsername()).isEqualTo("rita");
    }

    @Test
    void acceptsTheTokenWithoutTheBearerPrefix() {
        String token = tokenService().issueToken(1, 1, RoleConstants.ROLE_STUDENT, "sam");

        assertThat(tokenService().verifyToken(token)).isPresent();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        String token = new TokenService(OTHER_SECRET, 60)
                .issueToken(1, 1, RoleConstants.ROLE_ADMIN, "root");

        assertThat(tokenService().verifyToken(token)).isEmpty();
    }

    @Test
    void rejectsATamperedPayload() {
        String token = tokenService().issueToken(1, 2, RoleConstants.ROLE_STUDENT, "sam");
        String[] segments = token.split("\\.");
        String forged = segments[0] + "." + segments[1].substring(0, segments[1].length() - 2) + "AA." + segments[2];

        assertThat(tokenService().verifyToken(forged)).isEmpty();
    }

    @Test
    void rejectsMalformedTokens() {
        assertThat(tokenService().verifyToken(null)).isEmpty();
        assertThat(tokenService().verifyToken("")).isEmpty();
        assertThat(tokenService().verifyToken("Bearer ")).isEmpty();
        assertThat(tokenService().verifyToken("jwt-mock-42-1")).isEmpty();
        assertThat(tokenService().verifyToken("sc1.only-two-segments")).isEmpty();
        assertThat(tokenService().verifyToken("sc2.payload.signature")).isEmpty();
    }

    @Test
    void rejectsAnExpiredToken() {
        AuthPrincipal expired = new AuthPrincipal(1, 1, RoleConstants.ROLE_STUDENT, "sam",
                Instant.now().minusSeconds(1));
        String token = tokenService().issueToken(expired);

        assertThat(tokenService().verifyToken(token)).isEmpty();
    }

    @Test
    void rejectsAnUnknownRoleEvenWithAValidSignature() {
        String token = tokenService().issueToken(1, 1, "Superuser", "mallory");

        assertThat(tokenService().verifyToken(token)).isEmpty();
    }

    @Test
    void rejectsATokenWithoutARoleRow() {
        String token = tokenService().issueToken(1, 0, RoleConstants.ROLE_STUDENT, "sam");

        assertThat(tokenService().verifyToken(token)).isEmpty();
    }

    @Test
    void fallsBackToAnEphemeralSecretWhenNoneIsConfigured() {
        TokenService first = new TokenService("", 60);
        TokenService second = new TokenService("   ", 60);

        String token = first.issueToken(1, 1, RoleConstants.ROLE_STUDENT, "sam");

        assertThat(first.verifyToken(token)).isPresent();
        assertThat(second.verifyToken(token)).isEmpty();
    }

    @Test
    void aNonPositiveTimeToLiveNeverProducesAnAlreadyExpiredToken() {
        // A non positive configuration must never produce an already expired token. The
        // behaviour is asserted through the issued token rather than through a configuration
        // accessor, so it stays covered without a getter that nothing else calls.
        TokenService service = new TokenService(SECRET, 0);
        String token = service.issueToken(1, 1, RoleConstants.ROLE_STUDENT, "sam");

        AuthPrincipal principal = service.verifyToken(token).orElseThrow();

        assertThat(principal.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void aConfiguredTimeToLiveIsHonoured() {
        TokenService service = new TokenService(SECRET, 15);
        String token = service.issueToken(1, 1, RoleConstants.ROLE_STUDENT, "sam");

        AuthPrincipal principal = service.verifyToken(token).orElseThrow();

        assertThat(Duration.between(Instant.now(), principal.getExpiresAt()).toMinutes())
                .isBetween(13L, 15L);
    }
}
