package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.service.RegistrationRequestService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestControllerTest {

    private static final RequestPostProcessor ADMIN = TestAuth.asAdminAccount(30, 1);

    @Mock
    private RegistrationRequestService registrationRequestService;

    @InjectMocks
    private RegistrationRequestController registrationRequestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registrationRequestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    private static final String VALID_BODY = "{\"name\":\"Sam\",\"contact\":\"9876543210\",\"email\":\"sam@x.com\","
            + "\"username\":\"samuel\",\"password\":\"Passw0rd!\",\"requested_role\":\"Student\"}";

    private RegistrationRequest requestWith(int id, String name, String role, String status) {
        RegistrationRequest request = new RegistrationRequest();
        request.setRequestId(id);
        request.setName(name);
        request.setRequestedRole(role);
        request.setStatus(status);
        return request;
    }

    @Test
    void saveRequest_isPubliclyAvailable_withoutAToken() throws Exception {
        when(registrationRequestService.saveRequest(any(RegistrationRequest.class))).thenReturn(true);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(registrationRequestService).saveRequest(argThat(request -> "Pending".equals(request.getStatus())
                && "Student".equals(request.getRequestedRole())
                && "sam@x.com".equals(request.getEmail())));
    }

    @Test
    void saveRequest_returns400_whenUsernameAlreadyExists() throws Exception {
        when(registrationRequestService.saveRequest(any(RegistrationRequest.class)))
                .thenThrow(new BadRequestException("Username already exists."));

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists."));
    }

    @Test
    void saveRequest_returns400_whenAnEmailIsMissing() throws Exception {
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\",\"contact\":\"9876543210\",\"username\":\"samuel\","
                                + "\"password\":\"Passw0rd!\",\"requested_role\":\"Student\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please provide a valid email address"));
        verify(registrationRequestService, never()).saveRequest(any(RegistrationRequest.class));
    }

    @Test
    void saveRequest_returns400_whenThePasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("Passw0rd!", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be between 8 and 72 characters"));
    }

    @Test
    void saveRequest_returns400_whenTheRoleIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"Student\"", "\"Admin\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Requested role must be either Student or Teacher"));
    }

    @Test
    void saveRequest_returns500_whenSaveFails() throws Exception {
        when(registrationRequestService.saveRequest(any(RegistrationRequest.class))).thenReturn(false);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void saveRequest_returns500_whenServiceThrows() throws Exception {
        when(registrationRequestService.saveRequest(any(RegistrationRequest.class)))
                .thenThrow(new RuntimeException("duplicate key"));

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllRequests_requiresAToken() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllRequests_isAdminOnly() throws Exception {
        mockMvc.perform(get("/api/requests").with(TestAuth.asTeacher(10)))
                .andExpect(status().isForbidden());
        verify(registrationRequestService, never()).getAllRequests();
    }

    @Test
    void getAllRequests_returns404_whenEmpty() throws Exception {
        when(registrationRequestService.getAllRequests()).thenReturn(List.of());

        mockMvc.perform(get("/api/requests").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRequests_returnsRegisteredUsers() throws Exception {
        when(registrationRequestService.getAllRequests())
                .thenReturn(List.of(requestWith(3, "Sam", "Student", "Pending")));

        mockMvc.perform(get("/api/requests").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].request_id").value(3))
                .andExpect(jsonPath("$[0].name").value("Sam"))
                .andExpect(jsonPath("$[0].requested_role").value("Student"));
    }

    @Test
    void updateStatus_isAdminOnly() throws Exception {
        mockMvc.perform(put("/api/requests/5/status")
                        .with(TestAuth.asStudent(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Rejected\"}"))
                .andExpect(status().isForbidden());
        verify(registrationRequestService, never()).updateRequestStatus(5, "Rejected");
    }

    @Test
    void updateStatus_returns200_whenUpdated() throws Exception {
        when(registrationRequestService.updateRequestStatus(5, "Rejected")).thenReturn(true);

        mockMvc.perform(put("/api/requests/5/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Rejected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_returns400_whenTheStatusIsUnknown() throws Exception {
        mockMvc.perform(put("/api/requests/5/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Archived\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'Archived' is not a valid registration request status"));
    }

    @Test
    void updateStatus_returns404_whenNotFound() throws Exception {
        when(registrationRequestService.updateRequestStatus(99, "Rejected")).thenReturn(false);

        mockMvc.perform(put("/api/requests/99/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Rejected\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Request not found or could not be updated"));
    }

    @Test
    void rejectRequest_defaultsToRejected_whenNoBodyIsSent() throws Exception {
        when(registrationRequestService.updateRequestStatus(5, "Rejected")).thenReturn(true);

        mockMvc.perform(post("/api/requests/reject/5").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectRequest_returns404_whenMissing() throws Exception {
        when(registrationRequestService.updateRequestStatus(5, "Rejected")).thenReturn(false);

        mockMvc.perform(post("/api/requests/reject/5").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRequest_isAdminOnly() throws Exception {
        mockMvc.perform(delete("/api/requests/7").with(TestAuth.asStudent(7)))
                .andExpect(status().isForbidden());
        verify(registrationRequestService, never()).deleteRequest(7);
    }

    @Test
    void deleteRequest_returns200_whenDeleted() throws Exception {
        when(registrationRequestService.deleteRequest(7)).thenReturn(true);

        mockMvc.perform(delete("/api/requests/7").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRequest_returns404_whenMissing() throws Exception {
        when(registrationRequestService.deleteRequest(7)).thenReturn(false);

        mockMvc.perform(delete("/api/requests/7").with(ADMIN))
                .andExpect(status().isNotFound());
    }
}
