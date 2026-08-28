package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.RegistrationRequestRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestServiceTest {

    @Mock
    private RegistrationRequestRepository registrationRequestRepository;

    @InjectMocks
    private RegistrationRequestServiceImpl registrationRequestService;

    @Test
    void saveRequest_delegatesToRepository() {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Sam");
        when(registrationRequestRepository.saveRequest(request)).thenReturn(true);

        assertThat(registrationRequestService.saveRequest(request)).isTrue();
        verify(registrationRequestRepository).saveRequest(request);
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
    void updateRequestStatus_delegatesToRepository() {
        when(registrationRequestRepository.updateRequestStatus(4, "Approved")).thenReturn(true);

        assertThat(registrationRequestService.updateRequestStatus(4, "Approved")).isTrue();
        verify(registrationRequestRepository).updateRequestStatus(4, "Approved");
    }
}