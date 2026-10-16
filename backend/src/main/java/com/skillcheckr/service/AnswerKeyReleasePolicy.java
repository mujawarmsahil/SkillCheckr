package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.validation.ExamCreationValidator;

@Service
public class AnswerKeyReleasePolicy {

    public boolean isReleased(Exam exam) {
        LocalDateTime releaseAt = ExamCreationValidator.resolveWindowEnd(exam);
        return !LocalDateTime.now().isBefore(releaseAt);
    }

    public void protectStudentResult(ExamResultDTO result, Exam exam) {
        boolean released = isReleased(exam);
        result.setAnswerKeyReleased(released);
        if (!released) {
            result.setQuestionBreakdown(List.of());
        }
    }
}
