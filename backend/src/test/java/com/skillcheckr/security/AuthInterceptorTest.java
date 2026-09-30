package com.skillcheckr.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.constant.RoleConstants;
import com.skillcheckr.support.TestAuth;

class AuthInterceptorTest {

    @RestController
    @RequestMapping("/api/requests")
    static class ProbeController {

        @PostMapping({"", "/save"})
        public String save() {
            return "saved";
        }

        @GetMapping("")
        public String list() {
            return "list";
        }

        @DeleteMapping("/{id}")
        public String delete() {
            return "deleted";
        }

        @PutMapping("/status/{id}")
        public String status() {
            return "updated";
        }
    }

    @RestController
    @RequestMapping("/api/auth")
    static class LoginProbeController {

        @PostMapping("/login")
        public String login() {
            return "token";
        }

        @GetMapping("/login")
        public String loginPage() {
            return "must be protected";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController(), new LoginProbeController())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    @Test
    void signUpPostIsPublic() throws Exception {
        mockMvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("saved"));
    }

    @Test
    void theAdministrativeGetOnTheSignUpPathIsNotPublic() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherMethodsOnTheSignUpPathAreProtected() throws Exception {
        mockMvc.perform(delete("/api/requests/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/requests/status/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginPostIsPublicButLoginGetIsNot() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("token"));
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aMissingTokenIsRejectedWithAJsonMessage() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        String token = TestAuth.tokenFor(5, 5, RoleConstants.ROLE_ADMIN);
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/requests").with(TestAuth.withToken("Bearer " + tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        AuthPrincipal expired = new AuthPrincipal(1, 1, RoleConstants.ROLE_STUDENT, "student",
                java.time.Instant.now().minusSeconds(60));
        String expiredToken = TestAuth.tokenService().issueToken(expired);

        mockMvc.perform(get("/api/requests").with(TestAuth.withToken("Bearer " + expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithAnUnknownRoleIsRejected() throws Exception {
        mockMvc.perform(get("/api/requests").with(TestAuth.withRole("Root")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenPublishesThePrincipal() throws Exception {
        mockMvc.perform(get("/api/requests").with(TestAuth.asUser(30, RoleConstants.ROLE_TEACHER, 10)))
                .andExpect(status().isOk());
    }
}
