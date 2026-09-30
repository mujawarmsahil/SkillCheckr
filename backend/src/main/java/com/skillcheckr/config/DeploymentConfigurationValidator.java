package com.skillcheckr.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails the application at startup when a deployment that is not explicitly marked as a local
 * development environment is still running on insecure configuration.
 *
 * <p>Both checks exist because the unsafe behaviour was previously silent. The token secret used
 * to fall back to a value that is committed to this repository, so an environment that forgot to
 * supply one started normally, served valid tokens to anyone who had read the source, and only
 * revealed the problem in production. CORS used to append loopback wildcards on top of whatever
 * was configured, so the tightened origin list was widened again in every environment.
 */
@Configuration
public class DeploymentConfigurationValidator implements InitializingBean {

    /**
     * The development fallback secret. It is intentionally still recognised by name so that it can
     * be rejected outside a development profile, and it must never be used to sign real tokens.
     */
    public static final String DEVELOPMENT_FALLBACK_SECRET =
            "skillcheckr-local-development-secret-key-change-me";

    private static final int MINIMUM_SECRET_LENGTH = 32;

    /**
     * Profiles that explicitly opt in to the development fallback. Anything else, including no
     * active profile at all, is treated as a real deployment and must supply its own secret.
     */
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("dev", "local", "test");

    private static final Logger log = LoggerFactory.getLogger(DeploymentConfigurationValidator.class);

    private final Environment environment;
    private final String tokenSecret;
    private final String allowedOrigins;
    private final boolean allowCredentials;

    public DeploymentConfigurationValidator(Environment environment,
            @Value("${app.security.token-secret:}") String tokenSecret,
            @Value("${cors.allowed-origins:}") String allowedOrigins,
            @Value("${cors.allow-credentials:true}") boolean allowCredentials) {
        this.environment = environment;
        this.tokenSecret = tokenSecret;
        this.allowedOrigins = allowedOrigins;
        this.allowCredentials = allowCredentials;
    }

    @Override
    public void afterPropertiesSet() {
        validateTokenSecret();
        validateCorsOrigins();
    }

    private void validateTokenSecret() {
        String secret = tokenSecret == null ? "" : tokenSecret.trim();
        boolean development = isDevelopmentProfile();

        if (secret.isEmpty()) {
            if (development) {
                // TokenService already generates an ephemeral secret in this case, which is safe
                // but invalidates every issued token on restart. That is acceptable while
                // developing and must never be silently accepted anywhere else.
                log.warn("No app.security.token-secret configured. A random ephemeral secret is "
                        + "generated for this run, so all tokens stop working after a restart. "
                        + "Set TOKEN_SECRET to keep sessions across restarts.");
                return;
            }
            throw fail("app.security.token-secret is not set. Set the TOKEN_SECRET environment "
                    + "variable to at least " + MINIMUM_SECRET_LENGTH + " characters, for example "
                    + "with: openssl rand -base64 48");
        }

        if (DEVELOPMENT_FALLBACK_SECRET.equals(secret)) {
            if (development) {
                log.warn("Using the development fallback token secret. Tokens signed with it are "
                        + "forgeable by anyone who has read this repository. Never start a real "
                        + "deployment with this value.");
                return;
            }
            throw fail("app.security.token-secret is still the development fallback value that is "
                    + "committed to this repository. Every token would be forgeable. Set the "
                    + "TOKEN_SECRET environment variable to a private value of at least "
                    + MINIMUM_SECRET_LENGTH + " characters.");
        }

        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            if (development) {
                log.warn("Token secret is only {} characters; {} or more is recommended.",
                        secret.length(), MINIMUM_SECRET_LENGTH);
                return;
            }
            throw fail("app.security.token-secret is only " + secret.length() + " characters. "
                    + "At least " + MINIMUM_SECRET_LENGTH + " characters are required.");
        }

        log.info("Token secret is configured ({} characters).", secret.length());
    }

    /**
     * Catches the classic misconfiguration where credentials are enabled while any origin is
     * accepted. Spring permits this through origin patterns, and the browser then reflects the
     * requesting origin together with an allow credentials header, which lets a hostile page read
     * authenticated responses.
     */
    private void validateCorsOrigins() {
        if (!allowCredentials) {
            return;
        }
        Set<String> offending = new LinkedHashSet<>();
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> origin.equals("*") || origin.startsWith("*"))
                .forEach(offending::add);
        if (!offending.isEmpty()) {
            throw fail("cors.allowed-origins contains the wildcard " + offending
                    + " while cors.allow-credentials is enabled. The browser would then reflect "
                    + "any requesting origin with credentials allowed. List the exact origins "
                    + "instead, or set cors.allow-credentials=false.");
        }
    }

    private boolean isDevelopmentProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(DEVELOPMENT_PROFILES::contains);
    }

    private IllegalStateException fail(String message) {
        return new IllegalStateException("Refusing to start: " + message);
    }
}
