package com.skillcheckr.validation;

import com.skillcheckr.exception.BadRequestException;

/**
 * Converts loosely typed request values, such as the {@code Object} entries of a JSON body, into
 * numbers.
 *
 * <p>Request bodies are bound as maps of objects, so a numeric field can arrive as a number, as a
 * string, or as text that is not a number at all. Parsing those inline with
 * {@code Integer.parseInt} lets a {@link NumberFormatException} escape, which is an internal error
 * rather than a validation failure: the client is told the server broke, and the message can carry
 * the rejected value into the response. These helpers turn the same condition into the project's
 * ordinary {@link BadRequestException}, keeping the handling in the one place that already maps
 * business validation failures to a 400.
 */
public final class RequestValueParser {

    private RequestValueParser() {
    }

    /**
     * @param value   the raw value, which may be a number or a string
     * @param fieldName the field to name in the error message
     * @return the parsed integer
     * @throws BadRequestException when the value is not a number
     */
    public static int parseInt(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        if (value instanceof Integer number) {
            return number;
        }
        if (value instanceof Number number) {
            // A JSON decimal such as 7.5 for an id is not an integer request value.
            double asDouble = number.doubleValue();
            if (asDouble != Math.rint(asDouble) || Double.isInfinite(asDouble)) {
                throw new BadRequestException(fieldName + " must be a whole number");
            }
            return (int) asDouble;
        }

        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            // Text such as "4.0" is a whole number written with a decimal point, and "0.5" is a
            // number that is not whole. Neither deserves the same message as unparseable text, so
            // both are reported as precisely as the value allows.
            Double decimal = parseDecimalOrNull(raw);
            if (decimal != null) {
                if (decimal != Math.rint(decimal)) {
                    throw new BadRequestException(fieldName + " must be a whole number");
                }
                if (decimal >= Integer.MIN_VALUE && decimal <= Integer.MAX_VALUE) {
                    return decimal.intValue();
                }
            }
            // The value is deliberately not echoed back into the message.
            throw new BadRequestException(fieldName + " must be a valid number");
        }
    }

    private static Double parseDecimalOrNull(String raw) {
        try {
            double parsed = Double.parseDouble(raw);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * @param value     the raw value, which may be a number or a string
     * @param fieldName the field to name in the error message
     * @return the parsed integer, or null when the value is absent
     * @throws BadRequestException when the value is present but not a number
     */
    public static Integer parseOptionalInt(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        return parseInt(value, fieldName);
    }
}
