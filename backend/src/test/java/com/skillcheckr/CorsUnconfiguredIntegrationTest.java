package com.skillcheckr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the deny-by-default CORS behaviour when no origins are configured: an environment
 * that forgets {@code CORS_ALLOWED_ORIGINS} must reject every cross-origin request rather
 * than trust an origin that no operator wrote down.
 */
@SpringBootTest(properties = {
        "app.exam.attempt-finalizer-initial-delay-ms=3600000",
        "cors.allowed-origins="
})
@AutoConfigureMockMvc
class CorsUnconfiguredIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightIsRejectedWhenNoOriginsAreConfigured() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://skill-checkr.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
