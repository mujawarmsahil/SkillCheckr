package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.AuthRepository;
import com.skillcheckr.repository.RegistrationRequestRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestServiceTest {

    @Mock
    private RegistrationRequestRepository registrationRequestRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private RegistrationRequestServiceImpl registrationRequestService;

    private RegistrationRequest requestWithUsername(String username) {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Sam");
        request.setUsername(username);
        return request;
    }

    @Test
    void saveRequest_delegatesToRepository() {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Sam");
        when(registrationRequestRepository.saveRequest(request)).thenReturn(true);

        assertThat(registrationRequestService.saveRequest(request)).isTrue();
        verify(registrationRequestRepository).saveRequest(request);
    }

    @Test
    void saveRequest_rejects_whenUsernameExistsInUserTable() {
        RegistrationRequest request = requestWithUsername("alice");
        when(authRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> registrationRequestService.saveRequest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists.");
        verify(registrationRequestRepository, never()).saveRequest(request);
    }

    @Test
    void saveRequest_rejects_whenUsernameExistsInRequestTable() {
        RegistrationRequest request = requestWithUsername("bob");
        when(authRepository.existsByUsername("bob")).thenReturn(false);
        when(registrationRequestRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> registrationRequestService.saveRequest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists.");
        verify(registrationRequestRepository, never()).saveRequest(request);
    }

    @Test
    void saveRequest_succeeds_whenUsernameAvailable() {
        RegistrationRequest request = requestWithUsername("carol");
        when(authRepository.existsByUsername("carol")).thenReturn(false);
        when(registrationRequestRepository.existsByUsername("carol")).thenReturn(false);
        when(registrationRequestRepository.saveRequest(request)).thenReturn(true);

        assertThat(registrationRequestService.saveRequest(request)).isTrue();
        verify(registrationRequestRepository).saveRequest(request);
    }

    @Test
    void saveRequest_skipsUsernameCheck_whenUsernameBlank() {
        RegistrationRequest blank = requestWithUsername("   ");
        when(registrationRequestRepository.saveRequest(blank)).thenReturn(true);

        assertThat(registrationRequestService.saveRequest(blank)).isTrue();
        verify(authRepository, never()).existsByUsername(anyString());
        verify(registrationRequestRepository, never()).existsByUsername(anyString());
    }

    @Test
    void saveRequest_skipsUsernameCheck_whenUsernameNull() {
        RegistrationRequest noUsername = requestWithUsername(null);
        when(registrationRequestRepository.saveRequest(noUsername)).thenReturn(true);

        assertThat(registrationRequestService.saveRequest(noUsername)).isTrue();
        verify(authRepository, never()).existsByUsername(anyString());
        verify(registrationRequestRepository, never()).existsByUsername(anyString());
    }

    @Test
    void saveRequest_checksUntrimmedUsername() {
        RegistrationRequest request = requestWithUsername(" alice ");
        when(authRepository.existsByUsername(" alice ")).thenReturn(true);

        assertThatThrownBy(() -> registrationRequestService.saveRequest(request))
                .isInstanceOf(BadRequestException.class);
        verify(authRepository).existsByUsername(" alice ");
    }

    @Test
    void getAllRequests_delegatesToRepository() {
        when(registrationRequestRepository.getAllRequests())
                .thenReturn(List.of(new RegistrationRequest()));

        assertThat(registrationRequestService.getAllRequests()).hasSize(1);
        verify(registrationRequestRepository).getAllRequests();
    }

    @Test
    void deleteRequest_delegatesToRepository() {
        when(registrationRequestRepository.deleteRequestById(4)).thenReturn(true);

        assertThat(registrationRequestService.deleteRequest(4)).isTrue();
        verify(registrationRequestRepository).deleteRequestById(4);
    }

    @Test
    void updateRequestStatus_rejectsAPendingRequest() {
        when(registrationRequestRepository.getRequestStatus(4)).thenReturn(Optional.of("Pending"));
        when(registrationRequestRepository.updateRequestStatusIfCurrent(4, "Rejected", "Pending")).thenReturn(true);

        assertThat(registrationRequestService.updateRequestStatus(4, "Rejected")).isTrue();
        verify(registrationRequestRepository).updateRequestStatusIfCurrent(4, "Rejected", "Pending");
    }

    @Test
    void updateRequestStatus_normalisesTheTargetStatus() {
        when(registrationRequestRepository.getRequestStatus(4)).thenReturn(Optional.of("Pending"));
        when(registrationRequestRepository.updateRequestStatusIfCurrent(4, "Rejected", "Pending")).thenReturn(true);

        assertThat(registrationRequestService.updateRequestStatus(4, " rejected ")).isTrue();
    }

    @Test
    void updateRequestStatus_refusesToApproveWithoutCreatingTheAccount() {
        assertThatThrownBy(() -> registrationRequestService.updateRequestStatus(4, "Approved"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be marked approved directly");
        verify(registrationRequestRepository, never()).updateRequestStatusIfCurrent(anyInt(), anyString(), anyString());
    }

    @Test
    void updateRequestStatus_refusesAnUnknownStatus() {
        assertThatThrownBy(() -> registrationRequestService.updateRequestStatus(4, "Archived"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid target status");
    }

    @Test
    void updateRequestStatus_refusesToReopenAProcessedRequest() {
        when(registrationRequestRepository.getRequestStatus(4)).thenReturn(Optional.of("Rejected"));

        assertThatThrownBy(() -> registrationRequestService.updateRequestStatus(4, "Rejected"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only a pending registration request can be rejected");
        verify(registrationRequestRepository, never()).updateRequestStatusIfCurrent(anyInt(), anyString(), anyString());
    }

    @Test
    void updateRequestStatus_reportsAMissingRequest() {
        when(registrationRequestRepository.getRequestStatus(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationRequestService.updateRequestStatus(99, "Rejected"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Registration request not found");
    }

    @Test
    void updateRequestStatus_reportsAFailedTransition() {
        when(registrationRequestRepository.getRequestStatus(4)).thenReturn(Optional.of("Pending"));
        when(registrationRequestRepository.updateRequestStatusIfCurrent(4, "Rejected", "Pending")).thenReturn(false);

        assertThat(registrationRequestService.updateRequestStatus(4, "Rejected")).isFalse();
    }
}