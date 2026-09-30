package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.RegistrationRequest;

public interface RegistrationRequestRepository {

    boolean saveRequest(RegistrationRequest request);

    boolean existsByUsername(String username);

    List<RegistrationRequest> getAllRequests();

    boolean deleteRequestById(int id);

    boolean updateRequestStatus(int id, String status);

    /**
     * Updates the status only when the request is still in {@code expectedStatus}, so a
     * lifecycle transition cannot be applied twice.
     */
    boolean updateRequestStatusIfCurrent(int id, String status, String expectedStatus);

    Optional<String> getRequestStatus(int id);
}
