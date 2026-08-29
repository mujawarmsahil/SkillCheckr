package com.skillcheckr.repository;

import java.util.List;
import com.skillcheckr.model.Subject;

public interface SubjectRepository {
    List<Subject> getAllSubjects();
    Subject getSubjectById(int subjectId);
    Subject addSubject(Subject subject);
    boolean updateSubject(int subjectId, Subject subject);
    boolean deleteSubjectById(int subjectId);
    int getQuestionCountBySubjectId(int subjectId);
    int getExamCountBySubjectId(int subjectId);
}
