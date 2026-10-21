package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.persistence.entity.RegistrationRequestEntity;
import com.skillcheckr.persistence.repository.RegistrationRequestJpaRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationRequestRepositoryImplTest {

    @Mock
    private RegistrationRequestJpaRepository requestRepository;

    @InjectMocks
    private RegistrationRequestRepositoryImpl repository;

    @Test
    void saveRequest_persistsPendingRequestWithHashedPasswordAndGeneratedId() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        req.setContact("1234567890");
        req.setEmail("sam@test.com");
        req.setRequestedRole("Student");
        req.setUsername("sam");
        req.setPassword("$2a$10$hashedPass");
        req.setStatus("Approved");

        when(requestRepository.save(any(RegistrationRequestEntity.class))).thenAnswer(invocation -> {
            RegistrationRequestEntity entity = invocation.getArgument(0);
            entity.setRequestId(7);
            return entity;
        });

        assertThat(repository.saveRequest(req)).isTrue();

        ArgumentCaptor<RegistrationRequestEntity> captor = ArgumentCaptor.forClass(RegistrationRequestEntity.class);
        verify(requestRepository).save(captor.capture());
        RegistrationRequestEntity saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Sam");
        assertThat(saved.getContact()).isEqualTo("1234567890");
        assertThat(saved.getEmail()).isEqualTo("sam@test.com");
        assertThat(saved.getRequestedRole()).isEqualTo("Student");
        assertThat(saved.getUsername()).isEqualTo("sam");
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashedPass");
        assertThat(saved.getStatus()).isEqualTo("Pending");
    }

    @Test
    void saveRequest_returnsFalse_whenNoGeneratedId() {
        RegistrationRequest req = new RegistrationRequest();
        req.setName("Sam");
        when(requestRepository.save(any(RegistrationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(repository.saveRequest(req)).isFalse();
    }

    @Test
    void existsByUsername_isCaseInsensitiveAndNullSafe() {
        when(requestRepository.existsByUsernameIgnoreCase("sam")).thenReturn(true);

        assertThat(repository.existsByUsername("sam")).isTrue();
        assertThat(repository.existsByUsername(null)).isFalse();
        verify(requestRepository, never()).existsByUsernameIgnoreCase(null);
    }

    @Test
    void getAllRequests_mapsRowsWithoutExposingPasswords() {
        RegistrationRequestEntity entity = new RegistrationRequestEntity();
        entity.setRequestId(9);
        entity.setName("Sam");
        entity.setEmail("sam@test.com");
        entity.setUsername("sam");
        entity.setPassword("$2a$10$hashedPass");
        entity.setRequestedRole("Student");
        entity.setContact("123");
        entity.setStatus("Pending");
        when(requestRepository.findAllByOrderByRequestIdDesc()).thenReturn(List.of(entity));

        List<RegistrationRequest> list = repository.getAllRequests();

        assertThat(list).singleElement().satisfies(request -> {
            assertThat(request.getRequestId()).isEqualTo(9);
            assertThat(request.getName()).isEqualTo("Sam");
            assertThat(request.getUsername()).isEqualTo("sam");
            assertThat(request.getStatus()).isEqualTo("Pending");
            assertThat(request.getPassword()).isNull();
        });
    }

    @Test
    void deleteRequestById_returnsWhetherARowWasDeleted() {
        when(requestRepository.deleteByRequestId(5)).thenReturn(1);
        when(requestRepository.deleteByRequestId(6)).thenReturn(0);

        assertThat(repository.deleteRequestById(5)).isTrue();
        assertThat(repository.deleteRequestById(6)).isFalse();
    }

    @Test
    void updateRequestStatus_returnsWhetherARowWasUpdated() {
        when(requestRepository.updateStatus(5, "Approved")).thenReturn(1);

        assertThat(repository.updateRequestStatus(5, "Approved")).isTrue();
    }

    @Test
    void updateRequestStatusIfCurrent_delegatesTheCompareAndSet() {
        when(requestRepository.updateStatusIfCurrent(5, "Rejected", "Pending")).thenReturn(1);
        when(requestRepository.updateStatusIfCurrent(6, "Rejected", "Pending")).thenReturn(0);

        assertThat(repository.updateRequestStatusIfCurrent(5, "Rejected", "Pending")).isTrue();
        assertThat(repository.updateRequestStatusIfCurrent(6, "Rejected", "Pending")).isFalse();
    }

    @Test
    void getRequestStatus_readsTheStoredStatus() {
        RegistrationRequestEntity entity = new RegistrationRequestEntity();
        entity.setRequestId(5);
        entity.setStatus("Pending");
        when(requestRepository.findById(5)).thenReturn(Optional.of(entity));

        assertThat(repository.getRequestStatus(5)).contains("Pending");
    }

    @Test
    void getRequestStatus_isEmptyForMissingRequest() {
        when(requestRepository.findById(5)).thenReturn(Optional.empty());

        assertThat(repository.getRequestStatus(5)).isEmpty();
    }
}
