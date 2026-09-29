package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.service.RegistrationRequestService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/requests")
public class RegistrationRequestController {

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";

    @Autowired
    private RegistrationRequestService registrationRequestService;

    @PostMapping("")
    public ResponseEntity<ApiResponse> saveRequest(@RequestBody RegistrationRequest request) {
        // Public endpoint: this is how a visitor signs up for an account.
        validateRegistrationRequest(request);
        request.setStatus(STATUS_PENDING);
        if (!registrationRequestService.saveRequest(request)) {
            throw new IllegalStateException("Failed to submit registration request");
        }
        return ResponseEntity.ok(new ApiResponse(true,
                "Registration request submitted. Awaiting admin approval."));
    }

    @GetMapping("")
    public ResponseEntity<List<RegistrationRequest>> getAllRequests(HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        List<RegistrationRequest> requests = registrationRequestService.getAllRequests();
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{request_id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable("request_id") Integer requestId,
            @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : STATUS_REJECTED;
        requireKnownStatus(status);
        if (!registrationRequestService.updateRequestStatus(requestId, status)) {
            throw new ResourceNotFoundException("Request not found or could not be updated");
        }
        return ResponseEntity.ok(new ApiResponse(true, "Request status updated to " + status));
    }

    @PostMapping("/reject/{request_id}")
    public ResponseEntity<ApiResponse> rejectRequest(@PathVariable("request_id") Integer requestId,
            HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        if (!registrationRequestService.updateRequestStatus(requestId, STATUS_REJECTED)) {
            throw new ResourceNotFoundException("Request not found or could not be rejected");
        }
        return ResponseEntity.ok(new ApiResponse(true, "Request rejected and moved to archive"));
    }

    @DeleteMapping("/{request_id}")
    public ResponseEntity<ApiResponse> deleteRequest(@PathVariable("request_id") Integer requestId,
            HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        if (!registrationRequestService.deleteRequest(requestId)) {
            throw new ResourceNotFoundException("Request not found or could not be deleted");
        }
        return ResponseEntity.ok(new ApiResponse(true, "Request deleted successfully"));
    }

    private void requireKnownStatus(String status) {
        if (!STATUS_PENDING.equalsIgnoreCase(status) && !STATUS_APPROVED.equalsIgnoreCase(status)
                && !STATUS_REJECTED.equalsIgnoreCase(status)) {
            throw new BadRequestException("'" + status + "' is not a valid registration request status");
        }
    }

    private void validateRegistrationRequest(RegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration details are required");
        }
        if (isBlank(request.getName()) || request.getName().trim().length() > 100) {
            throw new BadRequestException("A name of at most 100 characters is required");
        }
        if (isBlank(request.getEmail()) || !request.getEmail().trim().matches("^[A-Za-z0-9+_.-]+@.+\\.[A-Za-z]{2,}$")) {
            throw new BadRequestException("Please provide a valid email address");
        }
        if (isBlank(request.getUsername()) || !request.getUsername().trim().matches("^[A-Za-z0-9._-]{4,50}$")) {
            throw new BadRequestException(
                    "Username must be 4 to 50 characters and may only contain letters, digits, dots, underscores and hyphens");
        }
        if (isBlank(request.getPassword()) || request.getPassword().length() < 8 || request.getPassword().length() > 72) {
            throw new BadRequestException("Password must be between 8 and 72 characters");
        }
        if (isBlank(request.getContact()) || !request.getContact().trim().matches("^[0-9+()\\- ]{7,20}$")) {
            throw new BadRequestException("Please provide a valid contact number");
        }
        String role = request.getRequestedRole() == null ? "" : request.getRequestedRole().trim();
        if (!"Student".equalsIgnoreCase(role) && !"Teacher".equalsIgnoreCase(role)) {
            throw new BadRequestException("Requested role must be either Student or Teacher");
        }

        request.setName(request.getName().trim());
        request.setEmail(request.getEmail().trim().toLowerCase());
        request.setUsername(request.getUsername().trim());
        request.setContact(request.getContact().trim());
        request.setRequestedRole("Student".equalsIgnoreCase(role) ? "Student" : "Teacher");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
