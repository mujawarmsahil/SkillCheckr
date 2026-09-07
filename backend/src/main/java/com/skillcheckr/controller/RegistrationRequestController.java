package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.service.RegistrationRequestService;

@RestController
@RequestMapping({"/api/requests", "/api/registration-requests"})
public class RegistrationRequestController {

    @Autowired
    private RegistrationRequestService registrationRequestService;

    @PostMapping({"/save", ""})
    public ResponseEntity<?> saveRequest(@RequestBody RegistrationRequest request) {
        try {
            if (request.getStatus() == null || request.getStatus().isEmpty()) {
                request.setStatus("Pending");
            }
            boolean result = registrationRequestService.saveRequest(request);
            if (result) {
                return ResponseEntity.ok(Map.of(
                        "message", "Registration request submitted successfully! Awaiting Admin approval.",
                        "success", true
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to submit registration request", "success", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage(), "success", false));
        }
    }

    @GetMapping({"/viewAllRegisterUsers", ""})
    public ResponseEntity<?> getAllRequests() {
        List<RegistrationRequest> list = registrationRequestService.getAllRequests();
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(list);
    }

    @PutMapping({"/status/{request_id}", "/{request_id}/status"})
    public ResponseEntity<?> updateStatus(
            @PathVariable("request_id") Integer requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : "Rejected";
        boolean updated = registrationRequestService.updateRequestStatus(requestId, status);
        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "message", "Request status updated to " + status,
                    "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Request not found or could not be updated", "success", false));
        }
    }

    @PostMapping({"/reject/{request_id}", "/rejectById/{request_id}"})
    public ResponseEntity<?> rejectRequest(@PathVariable("request_id") Integer requestId) {
        boolean updated = registrationRequestService.updateRequestStatus(requestId, "Rejected");
        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "message", "Request rejected and moved to archive",
                    "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Request not found or could not be rejected", "success", false));
        }
    }

    @DeleteMapping({"/deleteById/{request_id}", "/{request_id}"})
    public ResponseEntity<?> deleteRequest(@PathVariable("request_id") Integer requestId) {
        boolean b = registrationRequestService.deleteRequest(requestId);
        if (b) {
            return ResponseEntity.ok(Map.of("message", "Request deleted successfully", "success", true));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Request not found or could not be deleted", "success", false));
        }
    }
}
