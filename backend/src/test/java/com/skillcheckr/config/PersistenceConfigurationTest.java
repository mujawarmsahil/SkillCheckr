package com.skillcheckr.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "app.exam.attempt-finalizer-initial-delay-ms=3600000")
class PersistenceConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void openSessionInViewIsDisabledInTheRunningContext() {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void theBaseConfigurationPinsTheProductionPersistenceContract() throws IOException {
        Properties base = loadBaseApplicationProperties();
        assertThat(base.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(base.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(base.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(base.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
        assertThat(base.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void theRunningTestContextKeepsSchemaGenerationInsteadOfValidation() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isNotEqualTo("validate");
    }

    private Properties loadBaseApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new FileSystemResource("src/main/resources/application.properties").getInputStream()) {
            properties.load(in);
        }
        return properties;
    }
}
