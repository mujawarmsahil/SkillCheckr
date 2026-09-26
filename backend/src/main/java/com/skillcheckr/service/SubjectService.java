package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;
import com.skillcheckr.model.Subject;
import com.skillcheckr.model.SubjectStatsResponse;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Optional<Subject> getSubjectById(int subjectId);
    Subject addSubject(Subject subject);
    boolean updateSubject(int subjectId, Subject subject);
    boolean deleteSubjectById(int subjectId);
    List<SubjectStatsResponse> getAllSubjectsWithStats();
}
