package com.skillcheckr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies the {@code cors.*} properties bind into {@link CorsProperties}, the bean both the
 * CorsFilter and the startup validator consume. A binding regression would silently fall back
 * to the class defaults (no origins, credentials enabled).
 */
class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsPropertiesTest.TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class TestConfig {
    }

    @Test
    void defaultsToNoOriginsAndCredentialsEnabled() {
        contextRunner.run(context -> {
            CorsProperties properties = context.getBean(CorsProperties.class);
            assertThat(properties.getAllowedOrigins()).isEmpty();
            assertThat(properties.isAllowCredentials()).isTrue();
        });
    }

    @Test
    void bindsACommaSeparatedOriginList() {
        contextRunner
                .withPropertyValues(
                        "cors.allowed-origins=https://skill-checkr.vercel.app,http://localhost:5173")
                .run(context -> {
                    CorsProperties properties = context.getBean(CorsProperties.class);
                    assertThat(properties.getAllowedOrigins()).containsExactly(
                            "https://skill-checkr.vercel.app", "http://localhost:5173");
                });
    }

    @Test
    void bindsTheCredentialsOverride() {
        contextRunner.withPropertyValues("cors.allow-credentials=false")
                .run(context -> assertThat(
                        context.getBean(CorsProperties.class).isAllowCredentials()).isFalse());
    }

    @Test
    void anEmptyOriginValueBindsToAnEmptyList() {
        contextRunner.withPropertyValues("cors.allowed-origins=")
                .run(context -> assertThat(
                        context.getBean(CorsProperties.class).getAllowedOrigins()).isEmpty());
    }
}
