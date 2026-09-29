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
 * Covers the CORS behaviour of a real deployment. No profile is active here on purpose:
 * that is exactly the production default, and it must not trust loopback origins.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorsPreflightForVercelFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://skill-checkr.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://skill-checkr.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void loopbackOriginIsRejectedInTheProductionDefault() throws Exception {
        // A previous version appended http://localhost:* and http://127.0.0.1:* to every
        // configuration, so a deployed instance accepted authenticated cross-origin requests
        // from any loopback port. Those origins now only exist in the `dev` profile.
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void arbitraryLoopbackPortIsRejectedInTheProductionDefault() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:9999")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unrelatedSiteIsRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isForbidden());
    }
}
