package com.skillcheckr.repository;

import java.util.List;

import com.skillcheckr.model.RegistrationRequest;

public interface RegistrationRequestRepository {

    boolean saveRequest(RegistrationRequest request);

    List<RegistrationRequest> getAllRequests();

    boolean deleteRequestById(int id);

    boolean updateRequestStatus(int id, String status);
}
