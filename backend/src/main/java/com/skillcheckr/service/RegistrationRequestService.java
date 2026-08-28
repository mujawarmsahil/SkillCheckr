package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.RegistrationRequest;

public interface RegistrationRequestService {

    boolean saveRequest(RegistrationRequest request);

    List<RegistrationRequest> getAllRequests();

    boolean deleteRequest(int id);

    boolean updateRequestStatus(int id, String status);
}
