package com.skillcheckr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Exercises the real encoder bean. Service-level tests mock the encoder; this checks that
 * PasswordConfig produces BCrypt hashes and that verification accepts only the original
 * password.
 */
class PasswordConfigTest {

    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    @Test
    void encodesPlaintextIntoABcryptHash() {
        String hash = encoder.encode("correct horse battery staple");
        assertThat(hash)
                .isNotEqualTo("correct horse battery staple")
                .startsWith("$2");
    }

    @Test
    void matchesTheOriginalPasswordAndRejectsWrongOnes() {
        String hash = encoder.encode("s3cret-password");
        assertThat(encoder.matches("s3cret-password", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
