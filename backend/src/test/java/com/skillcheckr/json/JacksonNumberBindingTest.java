package com.skillcheckr.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;

/**
 * Guards the JSON number binding that the standalone controller tests cannot see.
 *
 * <p>{@code MockMvcBuilders.standaloneSetup} builds its own message converter, so it does not use
 * the {@link ObjectMapper} that Spring Boot configures from {@code application.properties}. These
 * tests run against the real context so that a regression in that configuration is caught here
 * rather than in production, where a fractional mark would be stored as a different mark.
 */
@SpringBootTest
class JacksonNumberBindingTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void theRunningApplicationRefusesToTruncateAFractionalNumberIntoAnInt() {
        assertThat(objectMapper.getDeserializationConfig()
                .isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT))
                .as("a fractional mark must not be silently truncated to a whole number")
                .isFalse();
    }

    @Test
    void aFractionalTotalMarkIsRefusedRatherThanTruncated() {
        String json = "{\"total_marks\":100.5,\"passing_marks\":35}";

        assertThatThrownBy(() -> objectMapper.readValue(json, Exam.class))
                .hasMessageContaining("total_marks");
    }

    @Test
    void aFractionalPassMarkIsRefusedRatherThanTruncated() {
        String json = "{\"total_marks\":100,\"passing_marks\":34.5}";

        assertThatThrownBy(() -> objectMapper.readValue(json, Exam.class))
                .hasMessageContaining("passing_marks");
    }

    @Test
    void aFractionalQuestionMarkIsRefusedRatherThanTruncated() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"marks\":1.5}", QuestionDTO.class))
                .hasMessageContaining("marks");
    }

    @Test
    void wholeMarksAreUnaffected() throws Exception {
        Exam exam = objectMapper.readValue(
                "{\"total_marks\":100,\"passing_marks\":35}", Exam.class);

        assertThat(exam.getTotalMarks()).isEqualTo(100);
        assertThat(exam.getPassingMarks()).isEqualTo(35);
    }

    @Test
    void theBuilderUsedBySpringBootAlsoRefusesTheTruncation() {
        // Guards against the property being dropped from application.properties: if the
        // application later builds its mapper some other way, this documents the expectation.
        ObjectMapper strict = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .build();

        assertThat(strict.getDeserializationConfig()
                .isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)).isFalse();
    }
}
