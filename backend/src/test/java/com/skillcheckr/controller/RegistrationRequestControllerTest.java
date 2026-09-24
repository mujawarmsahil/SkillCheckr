package com.skillcheckr.controller;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.service.RegistrationRequestService;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestControllerTest {

    @Mock
    private RegistrationRequestService registrationRequestService;

    @InjectMocks
    private RegistrationRequestController registrationRequestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registrationRequestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RegistrationRequest requestWith(int id, String name, String role, String status) {
        RegistrationRequest request = new RegistrationRequest();
        request.setRequestId(id);
        request.setName(name);
        request.setRequestedRole(role);
        request.setStatus(status);
        return request;
    }

    @Test
    void saveRequest_returns200_whenSaved() throws Exception {
        when(registrationRequestService.saveRequest(org.mockito.ArgumentMatchers.any(RegistrationRequest.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/requests/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\",\"contact\":\"9876543210\",\"email\":\"sam@x.com\","
                                + "\"username\":\"sam\",\"password\":\"Passw0rd!\","
                                + "\"requested_role\":\"Student\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void saveRequest_returns500_whenSaveFails() throws Exception {
        when(registrationRequestService.saveRequest(org.mockito.ArgumentMatchers.any(RegistrationRequest.class)))
                .thenReturn(false);

        mockMvc.perform(post("/api/requests/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void saveRequest_returns500_whenServiceThrows() throws Exception {
        when(registrationRequestService.saveRequest(org.mockito.ArgumentMatchers.any(RegistrationRequest.class)))
                .thenThrow(new RuntimeException("duplicate key"));

        mockMvc.perform(post("/api/requests/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sam\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllRequests_returns404_whenEmpty() throws Exception {
        when(registrationRequestService.getAllRequests()).thenReturn(List.of());

        mockMvc.perform(get("/api/requests/viewAllRegisterUsers"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRequests_returnsRegisteredUsers() throws Exception {
        when(registrationRequestService.getAllRequests())
                .thenReturn(List.of(requestWith(3, "Sam", "Student", "Pending")));

        mockMvc.perform(get("/api/requests/viewAllRegisterUsers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].request_id").value(3))
                .andExpect(jsonPath("$[0].name").value("Sam"))
                .andExpect(jsonPath("$[0].requested_role").value("Student"));
    }

    @Test
    void updateStatus_returns200_whenUpdated() throws Exception {
        when(registrationRequestService.updateRequestStatus(5, "Rejected")).thenReturn(true);

        mockMvc.perform(put("/api/requests/status/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Rejected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_returns404_whenNotFound() throws Exception {
        when(registrationRequestService.updateRequestStatus(99, "Rejected")).thenReturn(false);

        mockMvc.perform(put("/api/requests/status/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Rejected\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectRequest_returns200_whenRejected() throws Exception {
        when(registrationRequestService.updateRequestStatus(5, "Rejected")).thenReturn(true);

        mockMvc.perform(post("/api/requests/reject/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteRequest_returns200_whenDeleted() throws Exception {
        when(registrationRequestService.deleteRequest(7)).thenReturn(true);

        mockMvc.perform(delete("/api/requests/deleteById/7")).andExpect(status().isOk());
    }

    @Test
    void deleteRequest_returns404_whenMissing() throws Exception {
        when(registrationRequestService.deleteRequest(7)).thenReturn(false);

        mockMvc.perform(delete("/api/requests/deleteById/7")).andExpect(status().isNotFound());
    }
}