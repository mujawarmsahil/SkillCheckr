package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.message").value("Questions added successfully"));

        verify(questionService).saveQuestionsWithAnswers(org.mockito.ArgumentMatchers.argThat(
                questions -> questions.size() == 1
                        && "2+2?".equals(questions.get(0).getQuestion())
                        && questions.get(0).getSubjectId() == 1));
    }

    @Test
    void addAllQuestion_returns500_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("DB write failure")).when(questionService).saveQuestionsWithAnswers(any());

        mockMvc.perform(post("/api/create/addQues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"question\":\"test\"}]"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to add questions: DB write failure"));
    }

    @Test
    void getQuestionsBySubject_returnsQuestions() throws Exception {
        QuestionDTO q = new QuestionDTO();
        q.setQuestionId(1);
        q.setSubjectId(10);
        when(questionService.getQuestionsBySubjectId(10)).thenReturn(List.of(q));

        mockMvc.perform(get("/api/create/subject/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(1));
    }

    @Test
    void getQuestionsByExam_returnsQuestions() throws Exception {
        QuestionDTO q = new QuestionDTO();
        q.setQuestionId(2);
        q.setExamId(5);
        when(questionService.getQuestionsByExamId(5)).thenReturn(List.of(q));

        mockMvc.perform(get("/api/create/exam/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(2));
    }

    @Test
    void deleteQuestion_returns200_whenDeleted() throws Exception {
        when(questionService.deleteQuestionById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/create/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Question deleted successfully"));
    }

    @Test
    void deleteQuestion_returns404_whenNotFound() throws Exception {
        when(questionService.deleteQuestionById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/create/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Question not found"));
    }

    @Test
    void getAllQuestions_returnsOk() throws Exception {
        QuestionDTO q = QuestionDTO.builder().questionId(1).question("What is Java?").build();
        when(questionService.getAllQuestions()).thenReturn(List.of(q));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/questions/all"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].question").value("What is Java?"));
    }

    @Test
    void deleteQuestion_returnsOk_whenFound() throws Exception {
        when(questionService.deleteQuestionById(5)).thenReturn(true);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/questions/5"))
                .andExpect(status().isOk());
    }
}
