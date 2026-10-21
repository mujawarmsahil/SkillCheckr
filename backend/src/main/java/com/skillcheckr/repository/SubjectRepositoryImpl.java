package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.model.Subject;
import com.skillcheckr.persistence.entity.SubjectEntity;
import com.skillcheckr.persistence.repository.SubjectJpaRepository;

@Repository
public class SubjectRepositoryImpl implements SubjectRepository {

    private final SubjectJpaRepository subjectRepository;

    public SubjectRepositoryImpl(SubjectJpaRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAllByOrderBySubjectIdDesc().stream()
                .map(SubjectRepositoryImpl::toSubject)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subject> getSubjectById(int subjectId) {
        return subjectRepository.findById(subjectId).map(SubjectRepositoryImpl::toSubject);
    }

    @Override
    @Transactional
    public Subject addSubject(Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return null;
        }

        String name = subject.getSubjectName().trim();
        String code = subject.getSubjectCode() != null && !subject.getSubjectCode().trim().isEmpty()
                ? subject.getSubjectCode().trim().toUpperCase()
                : generateSubjectCode(name);

        if (subjectRepository.existsBySubjectCode(code)) {
            // Generate a unique suffix; a second collision still fails on the
            // uq_subject_code unique constraint, as it did with the JDBC insert.
            code = code + "-" + (int) (Math.random() * 900 + 100);
        }

        SubjectEntity entity = new SubjectEntity();
        entity.setSubjectName(name);
        entity.setSubjectCode(code);
        SubjectEntity saved = subjectRepository.save(entity);
        return new Subject(saved.getSubjectId(), name, code);
    }

    @Override
    @Transactional
    public boolean updateSubject(int subjectId, Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return false;
        }
        Optional<SubjectEntity> existing = subjectRepository.findById(subjectId);
        if (existing.isEmpty()) {
            return false;
        }

        String name = subject.getSubjectName().trim();
        String code = subject.getSubjectCode() != null && !subject.getSubjectCode().trim().isEmpty()
                ? subject.getSubjectCode().trim().toUpperCase()
                : generateSubjectCode(name);

        SubjectEntity entity = existing.get();
        entity.setSubjectName(name);
        entity.setSubjectCode(code);
        subjectRepository.save(entity);
        return true;
    }

    /**
     * FK-safe cascade in the exact order the previous JDBC implementation used:
     * guard on recorded attempts, then exam_question, answer, question, exam, subject.
     * The statements themselves stay native SQL on the repository because the child
     * tables have no entities yet.
     */
    @Override
    @Transactional
    public boolean deleteSubjectById(int subjectId) {
        List<Integer> examIds = subjectRepository.findExamIdsBySubjectId(subjectId);
        for (Integer examId : examIds) {
            if (subjectRepository.countAttemptsByExamId(examId) > 0) {
                throw new ExamInUseException(
                        "This subject has exams with recorded attempts and cannot be deleted.");
            }
        }

        for (Integer examId : examIds) {
            subjectRepository.deleteExamQuestionsByExamId(examId);
        }

        List<Integer> questionIds = subjectRepository.findQuestionIdsBySubjectId(subjectId);
        for (Integer questionId : questionIds) {
            subjectRepository.deleteAnswersByQuestionId(questionId);
        }
        subjectRepository.deleteQuestionsBySubjectId(subjectId);
        subjectRepository.deleteExamsBySubjectId(subjectId);

        return subjectRepository.deleteSubjectBySubjectId(subjectId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getQuestionCountBySubjectId(int subjectId) {
        return subjectRepository.countQuestionsBySubjectId(subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getExamCountBySubjectId(int subjectId) {
        return subjectRepository.countExamsBySubjectId(subjectId);
    }

    private static Subject toSubject(SubjectEntity entity) {
        return new Subject(entity.getSubjectId(), entity.getSubjectName(), entity.getSubjectCode());
    }

    private String generateSubjectCode(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 4) {
            base = base.substring(0, 4);
        }
        if (base.isEmpty()) {
            base = "SUB";
        }
        return base + ((int) (Math.random() * 900 + 100));
    }
}
