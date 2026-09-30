package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.AuthRepository;
import com.skillcheckr.repository.RegistrationRequestRepository;

@Service
public class RegistrationRequestServiceImpl implements RegistrationRequestService {

    private static final String USERNAME_TAKEN = "Username already exists.";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";

    @Autowired
    private RegistrationRequestRepository requestRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        request.setPassword(hashPassword(request.getPassword()));
        return requestRepository.saveRequest(request);
    }

    /**
     * Hashes a registration password with BCrypt unless it already carries a BCrypt prefix, in
     * which case it is stored as-is. The three-prefix check mirrors {@code AuthRepositoryImpl}.
     */
    private String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        if (rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$")
                || rawPassword.startsWith("$2y$")) {
            return rawPassword;
        }
        return passwordEncoder.encode(rawPassword);
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
        String target = status == null ? "" : status.trim();

        if (STATUS_APPROVED.equalsIgnoreCase(target)) {
            throw new BadRequestException(
                    "A request cannot be marked approved directly. Approve it from the approvals "
                            + "screen so the account is created as well.");
        }
        if (!STATUS_REJECTED.equalsIgnoreCase(target)) {
            throw new BadRequestException("'" + target + "' is not a valid target status for a registration request");
        }

        String current = requestRepository.getRequestStatus(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration request not found"));
        if (!STATUS_PENDING.equalsIgnoreCase(current)) {
            throw new BadRequestException("Only a pending registration request can be rejected, this one is '"
                    + current + "'");
        }

        return requestRepository.updateRequestStatusIfCurrent(id, STATUS_REJECTED, STATUS_PENDING);
    }
}
