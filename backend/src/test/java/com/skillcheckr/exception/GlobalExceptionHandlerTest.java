package com.skillcheckr.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static String messageOf(ResponseEntity<Map<String, Object>> response) {
        Object message = response.getBody().get("message");
        return message == null ? null : String.valueOf(message);
    }

    @Test
    void unexpectedException_isNotForwardedToTheClient() {
        String internal = "Table 'exam_application_system.user' doesn't exist; "
                + "SQL: SELECT status FROM user WHERE user_id = ?";

        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new DataAccessResourceFailureException(internal));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = messageOf(response);
        assertNotNull(message);
        assertFalse(message.contains("user"), "client response must not echo SQL details: " + message);
        assertFalse(message.toLowerCase().contains("sql"), message);
        assertFalse(message.toLowerCase().contains("table"), message);
    }

    @Test
    void unexpectedException_doesNotLeakDriverOrClassDetails() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(
                new BadSqlGrammarException("SELECT 1", "SELECT 1", new SQLException("boom")));

        String message = messageOf(response);
        assertNotNull(message);
        assertFalse(message.contains("com.skillcheckr"), message);
        assertFalse(message.contains("BadSqlGrammar"), message);
        assertFalse(message.contains("SELECT"), message);
    }

    @Test
    void unexpectedException_neverEchoesTheExceptionMessage() {
        String leaks = "Duplicate entry 'admin@x.com' for key 'user.username'";

        String message = messageOf(handler.handleGeneric(new DuplicateKeyException(leaks)));

        assertFalse(message.contains("admin@x.com"), message);
        assertFalse(message.contains("username"), message);
    }

    @Test
    void unexpectedException_withNullMessage_stillReturnsAGenericBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(new RuntimeException());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(messageOf(response));
    }

    @Test
    void numberFormatException_isA400RatherThanA500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNumberFormat(new NumberFormatException("For input string: \"abc\""));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(messageOf(response));
    }

    @Test
    void numberFormatException_doesNotEchoTheRejectedValue() {
        String rejected = "abc";

        String message = messageOf(
                handler.handleNumberFormat(new NumberFormatException("For input string: \"" + rejected + "\"")));

        assertFalse(message.contains(rejected), message);
    }

    @Test
    void badRequest_keepsItsOwnMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new BadRequestException("questionIds must be a valid number"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("questionIds must be a valid number", messageOf(response));
    }

    @Test
    void resourceNotFound_stays404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Exam not found with id: 9"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Exam not found with id: 9", messageOf(response));
    }

    @Test
    void attemptException_preservesItsOwnStatus() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAttemptException(new AttemptStartException(409, "This exam has already been submitted"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This exam has already been submitted", messageOf(response));
    }

    @Test
    void attemptException_keepsA404As404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAttemptException(new AttemptStartException(404, "Student not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Student not found", messageOf(response));
    }

    @Test
    void unauthorized_stays401() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnauthorized(new UnauthorizedException("Invalid credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", messageOf(response));
    }

    @Test
    void accountDisabled_stays403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccountDisabled(new AccountDisabledException("This account has been deactivated."));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("This account has been deactivated.", messageOf(response));
    }

    @Test
    void typeMismatch_stays400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleTypeMismatch(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid parameter value", messageOf(response));
    }

    @Test
    void unknownEndpoint_stays404RatherThanBecomingA500() {
        assertEquals(HttpStatus.NOT_FOUND, handler.handleNoHandlerFound(null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, handler.handleNoResourceFound(null).getStatusCode());
    }

    @Test
    void emptyResultOnLogin_stays401WithTheGenericCredentialMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleEmptyResult(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", messageOf(response));
    }

    @Test
    void genericMessageDoesNotVaryWithTheFailure() {
        String first = messageOf(handler.handleGeneric(new DataAccessResourceFailureException("a")));
        String second = messageOf(handler.handleGeneric(new IllegalStateException("b")));
        String third = messageOf(handler.handleGeneric(new NullPointerException()));

        assertEquals(first, second);
        assertEquals(second, third);
        assertTrue(first.length() > 0);
    }
}
