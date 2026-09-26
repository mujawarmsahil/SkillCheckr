package com.skillcheckr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.Subject;
import com.skillcheckr.model.SubjectStatsResponse;
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
    public Optional<Subject> getSubjectById(int subjectId) {
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
    public List<SubjectStatsResponse> getAllSubjectsWithStats() {
        List<Subject> subjects = subjectRepository.getAllSubjects();
        List<SubjectStatsResponse> subjectStats = new ArrayList<>();
        for (Subject subject : subjects) {
            subjectStats.add(SubjectStatsResponse.builder()
                    .subjectId(subject.getSubjectId())
                    .subjectName(subject.getSubjectName())
                    .subjectCode(subject.getSubjectCode())
                    .questionCount(subjectRepository.getQuestionCountBySubjectId(subject.getSubjectId()))
                    .examCount(subjectRepository.getExamCountBySubjectId(subject.getSubjectId()))
                    .build());
        }
        return subjectStats;
    }
}
