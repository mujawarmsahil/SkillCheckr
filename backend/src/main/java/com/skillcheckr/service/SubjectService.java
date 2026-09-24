package com.skillcheckr.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.skillcheckr.model.Subject;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Optional<Subject> getSubjectById(int subjectId);
    Subject addSubject(Subject subject);
    boolean updateSubject(int subjectId, Subject subject);
    boolean deleteSubjectById(int subjectId);
    List<Map<String, Object>> getAllSubjectsWithStats();
}
