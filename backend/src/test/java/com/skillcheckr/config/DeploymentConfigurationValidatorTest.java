package com.skillcheckr.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeploymentConfigurationValidatorTest {

    private static final String VALID_SECRET = "a-real-production-secret-of-sufficient-length";

    private MockEnvironment environmentWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        for (String profile : profiles) {
            environment.setActiveProfiles(profile);
        }
        return environment;
    }

    private DeploymentConfigurationValidator validator(MockEnvironment environment, String secret,
            String origins, boolean credentials) {
        return new DeploymentConfigurationValidator(environment, secret, origins, credentials);
    }

    @Test
    void aRealSecretIsAcceptedOutsideDevelopment() {
        assertThatCode(() -> validator(environmentWithProfiles("prod"), VALID_SECRET,
                "https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void aMissingSecretRefusesToStartOutsideDevelopment() {
        assertThatThrownBy(() -> validator(environmentWithProfiles("prod"), "",
                "https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_SECRET");
    }

    @Test
    void aMissingSecretAlsoRefusesToStartWhenNoProfileIsActive() {
        // The production image sets the prod profile, but a bare `java -jar` must not be a
        // silent way to run insecurely either.
        assertThatThrownBy(() -> validator(environmentWithProfiles(), "",
                "https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_SECRET");
    }

    @Test
    void theCommittedDevelopmentSecretIsRejectedOutsideDevelopment() {
        assertThatThrownBy(() -> validator(environmentWithProfiles("prod"),
                DeploymentConfigurationValidator.DEVELOPMENT_FALLBACK_SECRET,
                "https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development fallback");
    }

    @Test
    void aShortSecretIsRejectedOutsideDevelopment() {
        assertThatThrownBy(() -> validator(environmentWithProfiles("prod"), "too-short-secret",
                "https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }

    @Test
    void theDevelopmentFallbackIsAllowedUnderTheDevProfile() {
        assertThatCode(() -> validator(environmentWithProfiles("dev"),
                DeploymentConfigurationValidator.DEVELOPMENT_FALLBACK_SECRET,
                "http://localhost:5173", true).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void anEmptySecretIsAllowedUnderTheDevProfile() {
        assertThatCode(() -> validator(environmentWithProfiles("dev"), "",
                "http://localhost:5173", true).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void aWildcardOriginIsRejectedWhenCredentialsAreEnabled() {
        assertThatThrownBy(() -> validator(environmentWithProfiles("prod"), VALID_SECRET,
                "*,https://skill-checkr.vercel.app", true).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    void aWildcardOriginIsAllowedWhenCredentialsAreDisabled() {
        assertThatCode(() -> validator(environmentWithProfiles("prod"), VALID_SECRET,
                "*", false).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void loopbackOriginsAreNotRejectedByTheValidator() {
        // Loopback is a deployment decision made by the profile, not a startup failure, so the
        // validator stays out of the way and the CORS tests assert the per-profile behaviour.
        assertThatCode(() -> validator(environmentWithProfiles("prod"), VALID_SECRET,
                "https://skill-checkr.vercel.app,http://localhost:5173", true).afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
