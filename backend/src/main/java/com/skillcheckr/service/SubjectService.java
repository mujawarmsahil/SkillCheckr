package com.skillcheckr.service;

import java.util.List;
import java.util.Map;
import com.skillcheckr.model.Subject;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Subject getSubjectById(int subjectId);
    Subject addSubject(Subject subject);
    boolean updateSubject(int subjectId, Subject subject);
    boolean deleteSubjectById(int subjectId);
    List<Map<String, Object>> getAllSubjectsWithStats();
}
