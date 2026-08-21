package com.skillcheckr.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.service.QuestionService;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController questionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController).build();
    }

    @Test
    void addAllQuestion_returns200_andForwardsQuestionsToService() throws Exception {
        String payload = "[{\"subjectId\":1,\"question\":\"2+2?\","
                + "\"option1\":\"3\",\"option2\":\"4\",\"option3\":\"5\",\"option4\":\"6\","
                + "\"correctOption\":\"4\"}]";

        mockMvc.perform(post("/api/create/addQues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(questionService).saveQuestionsWithAnswers(org.mockito.ArgumentMatchers.argThat(
                questions -> questions.size() == 1
                        && "2+2?".equals(questions.get(0).getQuestion())
                        && questions.get(0).getSubjectId() == 1));
    }

    @Test
    void addAllQuestion_acceptsMultipleQuestions() throws Exception {
        String payload = "[{\"subjectId\":1,\"question\":\"Q1\",\"option1\":\"a\",\"option2\":\"b\","
                + "\"option3\":\"c\",\"option4\":\"d\",\"correctOption\":\"a\"},"
                + "{\"subjectId\":1,\"question\":\"Q2\",\"option1\":\"a\",\"option2\":\"b\","
                + "\"option3\":\"c\",\"option4\":\"d\",\"correctOption\":\"b\"}]";

        mockMvc.perform(post("/api/create/addQues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(questionService).saveQuestionsWithAnswers(org.mockito.ArgumentMatchers.argThat(
                questions -> questions.size() == 2));
    }
}