package com.skillcheckr.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.skillcheckr.model.Exam;
import com.skillcheckr.service.ExamService;

@Component("examAuthorization")
public class ExamAuthorizationService {

    private final ExamService examService;

    public ExamAuthorizationService(ExamService examService) {
        this.examService = examService;
    }

    public boolean canManage(Authentication authentication, int examId) {
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
        if (!p.isTeacher()) {
            return false;
        }
        return examService.getExamById(examId)
                .map(Exam::getTeacherId)
                .map(tid -> tid == p.getRoleId())
                .orElse(false);
    }
}
