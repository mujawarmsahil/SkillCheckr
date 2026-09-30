package com.skillcheckr.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    private static final int TEACHER_ID = 10;
    private static final int OTHER_TEACHER_ID = 11;

    @Mock
    private QuestionService questionService;

    @Mock
    private ExamService examService;

    @InjectMocks
    private QuestionController questionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    private Exam ownedExam(int examId) {
        Exam exam = new Exam();
        exam.setExamId(examId);
        exam.setTeacherId(TEACHER_ID);
        return exam;
    }

    @Test
    void questionEndpoints_rejectRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addAllQuestion_forwardsQuestionsToTheService() throws Exception {
        String payload = "[{\"subjectId\":1,\"question\":\"2+2?\","
                + "\"option1\":\"3\",\"option2\":\"4\",\"option3\":\"5\",\"option4\":\"6\","
                + "\"correctOption\":\"4\"}]";

        mockMvc.perform(post("/api/questions")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.message").value("Questions added successfully"));

        verify(questionService).saveQuestionsWithAnswers(argThat(
                questions -> questions.size() == 1
                        && "2+2?".equals(questions.get(0).getQuestion())
                        && questions.get(0).getSubjectId() == 1));
    }

    @Test
    void addAllQuestion_isNotAvailableToStudents() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(TestAuth.asStudent(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"question\":\"test\"}]"))
                .andExpect(status().isForbidden());
        verify(questionService, never()).saveQuestionsWithAnswers(any());
    }

    @Test
    void addAllQuestion_rejectsQuestionsForAnotherTeachersExam() throws Exception {
        when(examService.getExamById(5)).thenReturn(Optional.of(ownedExam(5)));

        mockMvc.perform(post("/api/questions")
                        .with(TestAuth.asTeacher(OTHER_TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"examId\":5,\"question\":\"test\"}]"))
                .andExpect(status().isForbidden());
        verify(questionService, never()).saveQuestionsWithAnswers(any());
    }

    @Test
    void addAllQuestion_rejectsAnEmptyList() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("At least one question is required"));
    }

    @Test
    void addAllQuestion_returns500_withoutLeakingTheCause_whenServiceThrows() throws Exception {
        // An unexpected failure is a server-side problem, so the client gets a generic message.
        // Echoing "DB write failure" back would tell a caller about internals, and a real failure
        // here would be a SQL error carrying table and column names.
        doThrow(new RuntimeException("DB write failure")).when(questionService).saveQuestionsWithAnswers(any());

        mockMvc.perform(post("/api/questions")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"question\":\"test\"}]"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(content().string(not(containsString("DB write failure"))));
    }

    @Test
    void getQuestionsBySubject_returnsQuestions() throws Exception {
        QuestionDTO q = new QuestionDTO();
        q.setQuestionId(1);
        q.setSubjectId(10);
        when(questionService.getQuestionsBySubjectId(10)).thenReturn(List.of(q));

        mockMvc.perform(get("/api/questions/subject/10").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(1));
    }

    @Test
    void getQuestionsByExam_returnsQuestions() throws Exception {
        QuestionDTO q = new QuestionDTO();
        q.setQuestionId(2);
        q.setExamId(5);
        when(questionService.getQuestionsByExamId(5)).thenReturn(List.of(q));

        mockMvc.perform(get("/api/questions/exam/5").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(2));
    }

    @Test
    void getAllQuestions_isStaffOnly() throws Exception {
        QuestionDTO q = QuestionDTO.builder().questionId(1).question("What is Java?").build();
        when(questionService.getAllQuestions()).thenReturn(List.of(q));

        mockMvc.perform(get("/api/questions").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("What is Java?"));
        mockMvc.perform(get("/api/questions").with(TestAuth.asStudent(7)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQuestionById_returns404_whenMissing() throws Exception {
        when(questionService.getQuestionById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/questions/99").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateQuestion_isAdminOnly() throws Exception {
        mockMvc.perform(put("/api/questions/1")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"updated\"}"))
                .andExpect(status().isForbidden());
        verify(questionService, never()).updateQuestion(any());
    }

    @Test
    void updateQuestion_returnsTheStoredQuestion() throws Exception {
        when(questionService.updateQuestion(any())).thenReturn(true);
        when(questionService.getQuestionById(1)).thenReturn(Optional.of(
                QuestionDTO.builder().questionId(1).question("updated").build()));

        mockMvc.perform(put("/api/questions/1")
                        .with(TestAuth.asAdmin(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("updated"));
    }

    @Test
    void deleteQuestion_returns200_whenDeleted() throws Exception {
        when(questionService.deleteQuestionById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/questions/1").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Question deleted successfully"));
    }

    @Test
    void deleteQuestion_returns404_whenNotFound() throws Exception {
        when(questionService.deleteQuestionById(99)).thenReturn(false);

        mockMvc.perform(delete("/api/questions/99").with(TestAuth.asAdmin(1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Question not found"));
    }

    @Test
    void deleteQuestion_isAdminOnly() throws Exception {
        mockMvc.perform(delete("/api/questions/5").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(questionService, never()).deleteQuestionById(5);
    }
}
