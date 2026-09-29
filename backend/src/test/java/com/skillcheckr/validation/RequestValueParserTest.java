package com.skillcheckr.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.skillcheckr.exception.BadRequestException;

class RequestValueParserTest {

    @Test
    void parsesIntegerValues() {
        assertEquals(7, RequestValueParser.parseInt(7, "examId"));
        assertEquals(7, RequestValueParser.parseInt("7", "examId"));
        assertEquals(7, RequestValueParser.parseInt(" 7 ", "examId"));
    }

    @Test
    void parsesLongValuesThatOverflowAnInt() {
        // A value beyond int range is a client error, not an internal one.
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt("99999999999", "examId"));
    }

    @Test
    void rejectsNonNumericTextAsAValidationError() {
        BadRequestException ex =
                assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt("abc", "examId"));

        assertEquals("examId must be a valid number", ex.getMessage());
    }

    @Test
    void rejectsInjectionStyleInput() {
        assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt("1; DROP TABLE user", "examId"));
        assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt("1 OR 1=1", "examId"));
    }

    @Test
    void rejectsAFractionalValueForAnIntegerField() {
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt("7.5", "examId"));
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt(7.5d, "examId"));
    }

    @Test
    void reportsAFractionalValueAsNotWholeRatherThanAsUnparseable() {
        // "0.5" is a number, just not a whole one, so the message should say so whether the
        // client sent it as a JSON number or as text.
        BadRequestException fromNumber = assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt(0.5d, "marks"));
        BadRequestException fromText = assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt("0.5", "marks"));

        assertEquals("marks must be a whole number", fromNumber.getMessage());
        assertEquals("marks must be a whole number", fromText.getMessage());
    }

    @Test
    void acceptsAWholeNumberWrittenWithADecimalPoint() {
        // 4.0 is still the whole number 4, so it must not be refused for its decimal point.
        assertEquals(4, RequestValueParser.parseInt("4.0", "marks"));
        assertEquals(4, RequestValueParser.parseInt(4.0d, "marks"));
    }

    @Test
    void stillReportsNonNumericTextAsInvalid() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt("abc", "marks"));
        assertEquals("marks must be a valid number", ex.getMessage());

        BadRequestException overflow = assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt("99999999999", "marks"));
        assertEquals("marks must be a valid number", overflow.getMessage());
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt(null, "examId"));
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt("   ", "examId"));
    }

    @Test
    void rejectsANonFiniteNumber() {
        assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt(Double.POSITIVE_INFINITY, "examId"));
    }

    @Test
    void rejectsTheValueItRefusedToParse() {
        // The offending value is attacker-influenced input, so it must not come back in the message.
        String secret = "s3cr3t-value";
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> RequestValueParser.parseInt(secret, "examId"));

        assertFalse(ex.getMessage().contains(secret), ex.getMessage());
    }

    @Test
    void optionalIntReturnsNullForAnAbsentValue() {
        assertNull(RequestValueParser.parseOptionalInt(null, "examId"));
    }

    @Test
    void optionalIntStillValidatesAPresentValue() {
        assertEquals(3, RequestValueParser.parseOptionalInt("3", "examId"));
        assertThrows(BadRequestException.class, () -> RequestValueParser.parseOptionalInt("nope", "examId"));
    }

    @Test
    void thereIsNoDecimalEntryPointLeftToRoundWith() {
        // Marks are whole numbers end to end, so the parser offers no way to accept a fractional
        // value and round it. A caller that needs a decimal would have to reintroduce one.
        assertThat(RequestValueParser.class.getDeclaredMethods())
                .filteredOn(method -> Modifier.isPublic(method.getModifiers()))
                .isNotEmpty()
                .allSatisfy(method -> assertThat(method.getReturnType())
                        .isIn(int.class, Integer.class));
    }

    @Test
    void everyRejectionIsTheProjectsOwnValidationException() {
        // A single exception type keeps all of these on the existing 400 path rather than
        // introducing a second error convention.
        assertTrue(BadRequestException.class.isInstance(
                assertThrows(BadRequestException.class, () -> RequestValueParser.parseInt("x", "f"))));
    }
}
