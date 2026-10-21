package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.persistence.entity.RegistrationRequestEntity;
import com.skillcheckr.persistence.repository.RegistrationRequestJpaRepository;

@Repository
public class RegistrationRequestRepositoryImpl implements RegistrationRequestRepository {

    static final String STATUS_PENDING = "Pending";

    private final RegistrationRequestJpaRepository requestRepository;

    public RegistrationRequestRepositoryImpl(RegistrationRequestJpaRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Override
    @Transactional
    public boolean saveRequest(RegistrationRequest request) {
        RegistrationRequestEntity entity = new RegistrationRequestEntity();
        entity.setName(request.getName());
        entity.setContact(request.getContact());
        entity.setEmail(request.getEmail());
        entity.setRequestedRole(request.getRequestedRole());
        entity.setUsername(request.getUsername());
        entity.setPassword(request.getPassword());
        // New requests always start Pending regardless of any status on the DTO.
        entity.setStatus(STATUS_PENDING);
        RegistrationRequestEntity saved = requestRepository.save(entity);
        return saved.getRequestId() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return username != null && requestRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationRequest> getAllRequests() {
        // Passwords are deliberately not copied: the list endpoint must not expose them.
        return requestRepository.findAllByOrderByRequestIdDesc().stream()
                .map(RegistrationRequestRepositoryImpl::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public boolean deleteRequestById(int id) {
        return requestRepository.deleteByRequestId(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateRequestStatus(int id, String status) {
        return requestRepository.updateStatus(id, status) > 0;
    }

    @Override
    @Transactional
    public boolean updateRequestStatusIfCurrent(int id, String status, String expectedStatus) {
        return requestRepository.updateStatusIfCurrent(id, status, expectedStatus) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRequestStatus(int id) {
        return requestRepository.findById(id).map(RegistrationRequestEntity::getStatus);
    }

    private static RegistrationRequest toSummary(RegistrationRequestEntity entity) {
        RegistrationRequest request = new RegistrationRequest();
        request.setRequestId(entity.getRequestId());
        request.setName(entity.getName());
        request.setContact(entity.getContact());
        request.setEmail(entity.getEmail());
        request.setUsername(entity.getUsername());
        request.setRequestedRole(entity.getRequestedRole());
        request.setStatus(entity.getStatus());
        return request;
    }
}
