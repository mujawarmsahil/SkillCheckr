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

import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.service.RegistrationRequestService;

@RestController
@RequestMapping({"/api/requests", "/api/registration-requests"})
public class RegistrationRequestController {

    @Autowired
    private RegistrationRequestService registrationRequestService;

    @PostMapping({"/save", ""})
    public ResponseEntity<ApiResponse> saveRequest(@RequestBody RegistrationRequest request) {
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            request.setStatus("Pending");
        }
        boolean result = registrationRequestService.saveRequest(request);
        if (result) {
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Registration request submitted. Awaiting admin approval."
            ));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to submit registration request"));
    }

    @GetMapping({"/viewAllRegisterUsers", ""})
    public ResponseEntity<List<RegistrationRequest>> getAllRequests() {
        List<RegistrationRequest> requests = registrationRequestService.getAllRequests();
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(requests);
    }

    @PutMapping({"/status/{request_id}", "/{request_id}/status"})
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable("request_id") Integer requestId,
            @RequestBody(required = false) Map<String, String> body) {
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : "Rejected";
        boolean updated = registrationRequestService.updateRequestStatus(requestId, status);
        if (updated) {
            return ResponseEntity.ok(new ApiResponse(true, "Request status updated to " + status));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Request not found or could not be updated"));
    }

    @PostMapping({"/reject/{request_id}", "/rejectById/{request_id}"})
    public ResponseEntity<ApiResponse> rejectRequest(@PathVariable("request_id") Integer requestId) {
        boolean updated = registrationRequestService.updateRequestStatus(requestId, "Rejected");
        if (updated) {
            return ResponseEntity.ok(new ApiResponse(true, "Request rejected and moved to archive"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Request not found or could not be rejected"));
    }

    @DeleteMapping({"/deleteById/{request_id}", "/{request_id}"})
    public ResponseEntity<ApiResponse> deleteRequest(@PathVariable("request_id") Integer requestId) {
        boolean deleted = registrationRequestService.deleteRequest(requestId);
        if (deleted) {
            return ResponseEntity.ok(new ApiResponse(true, "Request deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Request not found or could not be deleted"));
    }
}