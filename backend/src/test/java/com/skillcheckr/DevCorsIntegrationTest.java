package com.skillcheckr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the CORS behaviour under the `dev` profile, which is the explicit opt in for the
 * loopback origins the Vite dev server needs. Keeping this in a separate context proves the
 * loopback origins are granted by the profile rather than baked into the shared defaults.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorsPreflightForLocalhost() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void loopbackIpOriginIsAllowedForDevelopment() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://127.0.0.1:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void theProductionFrontendIsStillAllowedUnderTheDevProfile() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://skill-checkr.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://skill-checkr.vercel.app"));
    }

    @Test
    void unrelatedSiteIsStillRejectedUnderTheDevProfile() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isForbidden());
    }
}
