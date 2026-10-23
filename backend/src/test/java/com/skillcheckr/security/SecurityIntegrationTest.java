package com.skillcheckr.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skillcheckr.service.AuthService;
import com.skillcheckr.support.TestAuth;

@SpringBootTest(properties = "app.exam.attempt-finalizer-initial-delay-ms=3600000")
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void stubActive() {
        org.mockito.Mockito.when(authService.isUserActive(org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentTokenOnAdminEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .with(TestAuth.asStudent(1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenOnAdminEndpointIsAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightRemainsAllowed() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/admin/stats")
                .header("Origin", "https://skill-checkr.vercel.app")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
