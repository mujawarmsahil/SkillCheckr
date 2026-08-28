package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.RegistrationRequestRepository;

@Service
public class RegistrationRequestServiceImpl implements RegistrationRequestService {

    @Autowired
    private RegistrationRequestRepository requestRepository;

    @Override
    public boolean saveRequest(RegistrationRequest request) {
        return requestRepository.saveRequest(request);
    }

    @Override
    public List<RegistrationRequest> getAllRequests() {
        return requestRepository.getAllRequests();
    }

    @Override
    public boolean deleteRequest(int id) {
        return requestRepository.deleteRequestById(id);
    }

    @Override
    public boolean updateRequestStatus(int id, String status) {
        return requestRepository.updateRequestStatus(id, status);
    }
}
