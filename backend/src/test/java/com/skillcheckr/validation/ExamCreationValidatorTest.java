package com.skillcheckr.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.skillcheckr.exception.BadRequestException;

class ExamCreationValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Test
    void examDate_acceptsExactMinimumLeadTime() {
        assertThatCode(() -> ExamCreationValidator.validateExamDate("2026-10-05", TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void examDate_acceptsExactMinimumLeadTimeWithTime() {
        assertThatCode(() -> ExamCreationValidator.validateExamDate("2026-10-05T23:00", TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void examDate_acceptsDateBeyondMinimumLeadTime() {
        assertThatCode(() -> ExamCreationValidator.validateExamDate("2026-10-20T09:30", TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void examDate_rejectsDateOneDayBeforeMinimumLeadTime() {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamDate("2026-10-04T10:00", TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam date must be at least 10 days from today");
    }

    @Test
    void examDate_rejectsToday() {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamDate("2026-09-25T10:00", TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam date must be at least 10 days from today");
    }

    @Test
    void examDate_rejectsPastDate() {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamDate("2026-09-24T10:00", TODAY))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Java Programming", "Java-Programming", "Advanced Java", "Java" })
    void examName_acceptsLettersSpacesAndHyphens(String examName) {
        assertThatCode(() -> ExamCreationValidator.validateExamName(examName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Java 101", "Java@Programming", "Java_Programming", "Java#Exam", "Java--Programming",
            "Java  Programming", "-Java" })
    void examName_rejectsNumbersAndSpecialCharacters(String examName) {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamName(examName))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void examName_rejectsBlankValues(String examName) {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamName(examName))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam name is required");
    }

    @Test
    void examName_rejectsSurroundingWhitespace() {
        assertThatThrownBy(() -> ExamCreationValidator.validateExamName(" Java Programming "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam name must not start or end with a space");
    }
}
