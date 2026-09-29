package com.skillcheckr.controller;

import java.util.List;

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

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.Subject;
import com.skillcheckr.model.SubjectStatsResponse;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.service.SubjectService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping("")
    public ResponseEntity<List<Subject>> getAllSubjects(HttpServletRequest request) {
        AuthGuard.requirePrincipal(request);
        List<Subject> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects != null ? subjects : List.of());
    }

    @GetMapping("/with-stats")
    public ResponseEntity<List<SubjectStatsResponse>> getAllSubjectsWithStats(HttpServletRequest request) {
        AuthGuard.requireStaff(request);
        List<SubjectStatsResponse> subjectStats = subjectService.getAllSubjectsWithStats();
        return ResponseEntity.ok(subjectStats != null ? subjectStats : List.of());
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable("subjectId") Integer subjectId,
            HttpServletRequest request) {
        AuthGuard.requirePrincipal(request);
        Subject subject = subjectService.getSubjectById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return ResponseEntity.ok(subject);
    }

    @PostMapping("")
    public ResponseEntity<Subject> addSubject(@RequestBody Subject subject, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        validateSubject(subject);
        Subject saved = subjectService.addSubject(subject);
        if (saved == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{subjectId}")
    public ResponseEntity<Subject> updateSubject(@PathVariable("subjectId") Integer subjectId,
            @RequestBody Subject subject, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        validateSubject(subject);
        boolean updated = subjectService.updateSubject(subjectId, subject);
        if (!updated) {
            throw new ResourceNotFoundException("Subject not found or update failed");
        }
        return ResponseEntity.ok(subjectService.getSubjectById(subjectId).orElse(subject));
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<ApiResponse> deleteSubject(@PathVariable("subjectId") Integer subjectId,
            HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        boolean deleted = subjectService.deleteSubjectById(subjectId);
        if (deleted) {
            return ResponseEntity.ok(new ApiResponse(true, "Subject deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, "Subject not found or could not be deleted"));
    }

    private void validateSubject(Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            throw new BadRequestException("Subject name is required");
        }
        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            throw new BadRequestException("Subject code is required");
        }
        if (!subject.getSubjectCode().trim().matches("^[A-Za-z0-9-]{2,50}$")) {
            throw new BadRequestException("Subject code may only contain letters, digits and hyphens");
        }
        subject.setSubjectName(subject.getSubjectName().trim());
        subject.setSubjectCode(subject.getSubjectCode().trim().toUpperCase());
    }
}
