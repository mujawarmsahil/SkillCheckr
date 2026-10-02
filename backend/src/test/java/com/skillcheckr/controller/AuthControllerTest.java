package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import com.skillcheckr.exception.AccountDisabledException;
import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.User;
import com.skillcheckr.service.AuthService;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGIN_BODY = "{\"username\":\"%s\",\"password\":\"%s\"}";

    @BeforeEach
    void setUp() {
        // The real token service is used so the login response carries a verifiable signature.
        ReflectionTestUtils.setField(authController, "tokenService", TestAuth.tokenService());
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    /**
     * The student account id and the student row id differ in the database, so the ownership
     * checks are exercised with both values.
     */
    private static final RequestPostProcessor STUDENT = TestAuth.asStudentAccount(10, 7);
    private static final RequestPostProcessor ADMIN = TestAuth.asAdminAccount(30, 1);

    private User userWith(String username, String role, int userId) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setUserId(userId);
        return user;
    }

    @Test
    void login_returnsUnauthorized_whenUserDoesNotExist() throws Exception {
        when(authService.login("ghost", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("ghost", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returnsUnauthorized_whenCredentialsInvalidCauseEmptyResult() throws Exception {
        when(authService.login("ghost", "bad"))
                .thenThrow(new EmptyResultDataAccessException(1));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("ghost", "bad")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returnsForbidden_whenTheAccountWasDeactivated() throws Exception {
        when(authService.login("student1", "pass"))
                .thenThrow(new AccountDisabledException(
                        "This account has been deactivated. Please contact your administrator."));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("student1", "pass")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("This account has been deactivated. Please contact your administrator."));
    }

    @Test
    void login_doesNotIssueATokenForADeactivatedAccount() throws Exception {
        when(authService.login("student1", "pass"))
                .thenThrow(new AccountDisabledException("This account has been deactivated."));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("student1", "pass")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void login_returnsStudentPayload_whenRoleIsStudent() throws Exception {
        when(authService.login("student1", "pass")).thenReturn(Optional.of(userWith("student1", "Student", 10)));
        when(authService.getUserProfile(10)).thenReturn(Optional.empty());
        when(authService.getStudentIdByUserId(10)).thenReturn(7);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("student1", "pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login Successful"))
                .andExpect(jsonPath("$.role").value("Student"))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.roleId").value(7));

        verify(authService).getStudentIdByUserId(10);
        verify(authService, never()).getTeacherIdByUserId(anyInt());
        verify(authService, never()).getAdminIdByUserId(anyInt());
    }

    @Test
    void login_returnsTeacherPayload_whenRoleIsTeacher() throws Exception {
        when(authService.login("teacher1", "pass")).thenReturn(Optional.of(userWith("teacher1", "Teacher", 20)));
        when(authService.getUserProfile(20)).thenReturn(Optional.empty());
        when(authService.getTeacherIdByUserId(20)).thenReturn(5);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("teacher1", "pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Teacher"))
                .andExpect(jsonPath("$.roleId").value(5))
                .andExpect(jsonPath("$.userId").value(20));

        verify(authService).getTeacherIdByUserId(20);
        verify(authService, never()).getStudentIdByUserId(anyInt());
        verify(authService, never()).getAdminIdByUserId(anyInt());
    }

    @Test
    void login_returnsAdminPayload_whenRoleIsAdmin() throws Exception {
        when(authService.login("admin1", "pass")).thenReturn(Optional.of(userWith("admin1", "Admin", 30)));
        when(authService.getUserProfile(30)).thenReturn(Optional.empty());
        when(authService.getAdminIdByUserId(30)).thenReturn(1);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("admin1", "pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Admin"))
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.userId").value(30));

        verify(authService).getAdminIdByUserId(30);
        verify(authService, never()).getStudentIdByUserId(anyInt());
        verify(authService, never()).getTeacherIdByUserId(anyInt());
    }

    @Test
    void login_returnsZeroRoleId_whenRoleIsUnknown() throws Exception {
        when(authService.login("mystery", "pass")).thenReturn(Optional.of(userWith("mystery", "Supervisor", 40)));
        when(authService.getUserProfile(40)).thenReturn(Optional.empty());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("mystery", "pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(0))
                .andExpect(jsonPath("$.userId").value(40));

        verify(authService, never()).getStudentIdByUserId(anyInt());
        verify(authService, never()).getTeacherIdByUserId(anyInt());
        verify(authService, never()).getAdminIdByUserId(anyInt());
    }

    @Test
    void getUserProfile_returnsProfile_whenUserExists() throws Exception {
        com.skillcheckr.model.UserProfileDTO profile = com.skillcheckr.model.UserProfileDTO.builder()
                .userId(10)
                .username("student1")
                .name("Alice Student")
                .email("alice@test.com")
                .role("Student")
                .roleId(7)
                .build();
        when(authService.getUserProfile(10)).thenReturn(Optional.of(profile));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/profile/10").with(STUDENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(10))
                .andExpect(jsonPath("$.username").value("student1"))
                .andExpect(jsonPath("$.name").value("Alice Student"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    void getUserProfile_returns404_whenUserDoesNotExist() throws Exception {
        when(authService.getUserProfile(999)).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/profile/999").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserProfile_isRefusedForAnotherUsersAccount() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/profile/999").with(STUDENT))
                .andExpect(status().isForbidden());
        verify(authService, never()).getUserProfile(999);
    }

    @Test
    void getUserProfile_requiresAToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/profile/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_returnsSuccess_whenDataIsValid_withoutPasswordChange() throws Exception {
        com.skillcheckr.model.UserProfileDTO updated = com.skillcheckr.model.UserProfileDTO.builder()
                .userId(10)
                .username("student1_updated")
                .name("Alice Updated")
                .email("alice_updated@test.com")
                .role("Student")
                .build();

        when(authService.isUsernameInUse("student1_updated", 10)).thenReturn(false);
        when(authService.isEmailInUse("alice_updated@test.com", 10)).thenReturn(false);
        when(authService.updateUserProfile(org.mockito.ArgumentMatchers.any(com.skillcheckr.model.UserProfileDTO.class))).thenReturn(Optional.of(updated));

        String json = "{\"username\":\"student1_updated\",\"name\":\"Alice Updated\",\"email\":\"alice_updated@test.com\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated."))
                .andExpect(jsonPath("$.profile.name").value("Alice Updated"));
    }

    @Test
    void updateProfile_returnsSuccess_whenDataIsValid_withValidOldPassword() throws Exception {
        com.skillcheckr.model.UserProfileDTO updated = com.skillcheckr.model.UserProfileDTO.builder()
                .userId(10)
                .username("student1_updated")
                .name("Alice Updated")
                .email("alice_updated@test.com")
                .role("Student")
                .build();

        when(authService.isUsernameInUse("student1_updated", 10)).thenReturn(false);
        when(authService.isEmailInUse("alice_updated@test.com", 10)).thenReturn(false);
        when(authService.verifyCurrentPassword(10, "oldSecret123")).thenReturn(true);
        when(authService.updateUserProfile(org.mockito.ArgumentMatchers.any(com.skillcheckr.model.UserProfileDTO.class))).thenReturn(Optional.of(updated));

        String json = "{\"username\":\"student1_updated\",\"name\":\"Alice Updated\",\"email\":\"alice_updated@test.com\",\"old_password\":\"oldSecret123\",\"password\":\"newSecret123\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated."))
                .andExpect(jsonPath("$.profile.name").value("Alice Updated"));
    }

    @Test
    void updateProfile_rejectsPasswordsShorterThanEightCharacters() throws Exception {
        when(authService.isUsernameInUse("student1_updated", 10)).thenReturn(false);
        when(authService.isEmailInUse("alice_updated@test.com", 10)).thenReturn(false);

        String json = "{\"username\":\"student1_updated\",\"name\":\"Alice Updated\",\"email\":\"alice_updated@test.com\",\"old_password\":\"oldSecret123\",\"password\":\"short\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be at least 8 characters long"));
    }

    @Test
    void updateProfile_returnsBadRequest_whenOldPasswordInvalid() throws Exception {
        when(authService.isUsernameInUse("student1_updated", 10)).thenReturn(false);
        when(authService.isEmailInUse("alice_updated@test.com", 10)).thenReturn(false);
        when(authService.verifyCurrentPassword(10, "wrongOldPass")).thenReturn(false);

        String json = "{\"username\":\"student1_updated\",\"name\":\"Alice Updated\",\"email\":\"alice_updated@test.com\",\"current_password\":\"wrongOldPass\",\"password\":\"newSecret123\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid current password. Cannot change password."));
    }

    @Test
    void updateProfile_returnsBadRequest_whenOldPasswordMissingOnPasswordChange() throws Exception {
        when(authService.isUsernameInUse("student1_updated", 10)).thenReturn(false);
        when(authService.isEmailInUse("alice_updated@test.com", 10)).thenReturn(false);

        String json = "{\"username\":\"student1_updated\",\"name\":\"Alice Updated\",\"email\":\"alice_updated@test.com\",\"password\":\"newSecret123\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is required to change password"));
    }

    @Test
    void updateProfile_returnsBadRequest_whenUsernameInUse() throws Exception {
        when(authService.isUsernameInUse("existing_user", 10)).thenReturn(true);

        String json = "{\"username\":\"existing_user\",\"name\":\"Alice\",\"email\":\"alice@test.com\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken by another account"));
    }

    @Test
    void updateProfile_returnsBadRequest_whenEmailInUse() throws Exception {
        when(authService.isUsernameInUse("alice", 10)).thenReturn(false);
        when(authService.isEmailInUse("existing@test.com", 10)).thenReturn(true);

        String json = "{\"username\":\"alice\",\"name\":\"Alice\",\"email\":\"existing@test.com\"}";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/profile/10")
                        .with(STUDENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already in use by another account"));
    }
}