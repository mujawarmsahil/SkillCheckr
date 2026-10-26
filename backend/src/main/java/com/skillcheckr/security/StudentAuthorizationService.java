package com.skillcheckr.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.repository.ExamAttemptRepository;

@Component("studentAuthorization")
public class StudentAuthorizationService {

    private final ExamAttemptRepository examAttemptRepository;

    public StudentAuthorizationService(ExamAttemptRepository examAttemptRepository) {
        this.examAttemptRepository = examAttemptRepository;
    }

    public boolean canAccessStudentData(Authentication authentication, int studentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthPrincipal p)) {
            return false;
        }
        if (p.isAdmin()) {
            return true;
        }
        return p.isStudent() && p.getRoleId() == studentId;
    }

    public boolean canAccessAttempt(Authentication authentication, int attemptId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthPrincipal p)) {
            return false;
        }
        if (p.isAdmin()) {
            return true;
        }
        if (!p.isStudent()) {
            return false;
        }
        ExamAttempt attempt = examAttemptRepository.findById(attemptId);
        return attempt != null && attempt.getStudent() != null
                && attempt.getStudent().getStudentId() == p.getRoleId();
    }
}
