package com.skillcheckr.service;

import org.springframework.stereotype.Service;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamRepository;

@Service
public class QuestionPaperMutationPolicy {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public QuestionPaperMutationPolicy(ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository) {
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    public Exam lockAndValidate(int examId) {
        Exam exam = examRepository.getExamByIdForUpdate(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
        validate(exam);
        return exam;
    }

    public void validate(Exam exam) {
        if (exam.isPaperFrozen() || ExamConstants.isOpenForRegistration(exam.getStatus())
                || ExamConstants.EXAM_STATUS_COMPLETED.equalsIgnoreCase(exam.getStatus())) {
            throw new ExamInUseException("The approved exam question paper is frozen");
        }
        if (!ExamConstants.EXAM_STATUS_PENDING.equalsIgnoreCase(exam.getStatus())) {
            throw new ExamInUseException("The exam question paper can only be changed while pending approval");
        }
        if (examAttemptRepository.hasAttemptsForExam(exam.getExamId())) {
            throw new ExamInUseException("The exam question paper cannot be changed after an attempt exists");
        }
    }

}
