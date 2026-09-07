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

import com.skillcheckr.model.Subject;
import com.skillcheckr.service.SubjectService;

@RestController
@RequestMapping({"/api/subjects", "/api/Subjects"})
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping({"", "/all"})
    public ResponseEntity<?> getAllSubjects() {
        List<Subject> list = subjectService.getAllSubjects();
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping("/with-stats")
    public ResponseEntity<?> getAllSubjectsWithStats() {
        List<Map<String, Object>> list = subjectService.getAllSubjectsWithStats();
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<?> getSubjectById(@PathVariable("subjectId") Integer subjectId) {
        Subject subject = subjectService.getSubjectById(subjectId);
        if (subject != null) {
            return ResponseEntity.ok(subject);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Subject not found", "success", false));
    }

    @PostMapping({"", "/add"})
    public ResponseEntity<?> addSubject(@RequestBody Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Subject name is required", "success", false));
        }
        Subject saved = subjectService.addSubject(subject);
        if (saved != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to create subject", "success", false));
    }

    @PutMapping("/{subjectId}")
    public ResponseEntity<?> updateSubject(@PathVariable("subjectId") Integer subjectId, @RequestBody Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Subject name is required", "success", false));
        }
        boolean updated = subjectService.updateSubject(subjectId, subject);
        if (updated) {
            Subject saved = subjectService.getSubjectById(subjectId);
            return ResponseEntity.ok(saved != null ? saved : Map.of("message", "Subject updated successfully", "success", true));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Subject not found or update failed", "success", false));
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<?> deleteSubject(@PathVariable("subjectId") Integer subjectId) {
        boolean deleted = subjectService.deleteSubjectById(subjectId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Subject and associated data deleted successfully", "success", true));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Subject not found or could not be deleted", "success", false));
    }
}
