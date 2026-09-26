package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.AuthRepository;
import com.skillcheckr.repository.RegistrationRequestRepository;

@Service
public class RegistrationRequestServiceImpl implements RegistrationRequestService {

    private static final String USERNAME_TAKEN = "Username already exists.";

    @Autowired
    private RegistrationRequestRepository requestRepository;

    @Autowired
    private AuthRepository authRepository;

    @Override
    public boolean saveRequest(RegistrationRequest request) {
        String username = request.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            if (authRepository.existsByUsername(username)) {
                throw new BadRequestException(USERNAME_TAKEN);
            }
            if (requestRepository.existsByUsername(username)) {
                throw new BadRequestException(USERNAME_TAKEN);
            }
        }
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
