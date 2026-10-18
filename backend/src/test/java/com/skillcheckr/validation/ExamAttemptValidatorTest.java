package com.skillcheckr.validation;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Student;

class ExamAttemptValidatorTest {

    @Test
    void answerOwnershipValidationLeavesExpiryToAttemptExpiryPolicy() {
        ExamAttempt attempt = ExamAttempt.builder()
                .exam(Exam.builder().examId(10).build())
                .student(Student.builder().studentId(20).build())
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .status("IN_PROGRESS")
                .build();

        assertThatCode(() -> ExamAttemptValidator.validateAnswerAttempt(attempt, 10, 20))
                .doesNotThrowAnyException();
    }
}
