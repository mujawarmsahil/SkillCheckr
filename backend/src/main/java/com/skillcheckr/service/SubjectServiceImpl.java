package com.skillcheckr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.SubjectRepository;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public List<Subject> getAllSubjects() {
        return subjectRepository.getAllSubjects();
    }

    @Override
    public Subject getSubjectById(int subjectId) {
        return subjectRepository.getSubjectById(subjectId);
    }

    @Override
    public Subject addSubject(Subject subject) {
        return subjectRepository.addSubject(subject);
    }

    @Override
    public boolean updateSubject(int subjectId, Subject subject) {
        return subjectRepository.updateSubject(subjectId, subject);
    }

    @Override
    public boolean deleteSubjectById(int subjectId) {
        return subjectRepository.deleteSubjectById(subjectId);
    }

    @Override
    public List<Map<String, Object>> getAllSubjectsWithStats() {
        List<Subject> subjects = subjectRepository.getAllSubjects();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Subject s : subjects) {
            Map<String, Object> map = new HashMap<>();
            map.put("subjectId", s.getSubjectId());
            map.put("subject_id", s.getSubjectId());
            map.put("subjectName", s.getSubjectName());
            map.put("subject_name", s.getSubjectName());
            map.put("subjectCode", s.getSubjectCode());
            map.put("subject_code", s.getSubjectCode());
            map.put("questionCount", subjectRepository.getQuestionCountBySubjectId(s.getSubjectId()));
            map.put("examCount", subjectRepository.getExamCountBySubjectId(s.getSubjectId()));
            list.add(map);
        }
        return list;
    }
}
