package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.validation.ExamCreationValidator;

@Service
public class AnswerKeyReleasePolicy {

    @Autowired(required = false)
    private Clock clock;

    public boolean isReleased(Exam exam) {
        LocalDateTime releaseAt = ExamCreationValidator.resolveWindowEnd(exam);
        LocalDateTime now = clock == null ? LocalDateTime.now() : LocalDateTime.now(clock);
        return !now.isBefore(releaseAt);
    }

    public void protectStudentResult(ExamResultDTO result, Exam exam) {
        boolean released = isReleased(exam);
        result.setAnswerKeyReleased(released);
        if (!released) {
            result.setQuestionBreakdown(List.of());
        }
    }
}
