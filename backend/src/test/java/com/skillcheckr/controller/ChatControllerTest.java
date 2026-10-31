package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.ChatResponse;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.service.AiChatService;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final int STUDENT_ID = 7;
    private static final int EXAM_ID = 5;
    private static final int QUESTION_ID = 9;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private ExamService examService;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper strictMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(strictMapper))
                .build();
    }

    private MockHttpServletRequestBuilder chatRequest(String body) throws Exception {
        return post("/api/chat")
                .with(TestAuth.asStudent(STUDENT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void chat_returnsAnswerForAuthenticatedCaller() throws Exception {
        when(aiChatService.chat(any(), any(), any()))
                .thenReturn(ChatResponse.builder().answered(true).answer("Photosynthesis is the process of converting light energy.").build());

        mockMvc.perform(chatRequest("{\"message\":\"Explain photosynthesis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(true))
                .andExpect(jsonPath("$.answer").value("Photosynthesis is the process of converting light energy."));
    }

    @Test
    void chat_returnsNotAvailableWhenAssistantCannotAnswer() throws Exception {
        when(aiChatService.chat(any(), any(), any()))
                .thenReturn(ChatResponse.builder().answered(false).answer(null).build());

        mockMvc.perform(chatRequest("{\"message\":\"What is the capital of France?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(false));
    }

    @Test
    void chat_rejectsRequestWithoutMessage() throws Exception {
        when(aiChatService.chat(any(), any(), any())).thenThrow(new BadRequestException("A chat message is required"));

        mockMvc.perform(chatRequest("{\"examId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_studentCannotAskAboutExamOutsideRegistrationWindow() throws Exception {
        Exam pendingExam = new Exam();
        pendingExam.setExamId(EXAM_ID);
        pendingExam.setExamName("Physics");
        pendingExam.setStatus("Pending");
        when(examService.getExamById(EXAM_ID)).thenReturn(Optional.of(pendingExam));

        mockMvc.perform(post("/api/chat")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Can you help me prepare?\",\"examId\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chat_studentContextNeverIncludesAnswerKey() throws Exception {
        Exam approvedExam = new Exam();
        approvedExam.setExamId(EXAM_ID);
        approvedExam.setExamName("Physics");
        approvedExam.setStatus("Approved");
        when(examService.getExamById(EXAM_ID)).thenReturn(Optional.of(approvedExam));
        QuestionDTO question = new QuestionDTO();
        question.setQuestionId(QUESTION_ID);
        question.setQuestion("What is the powerhouse of the cell?");
        question.setCorrectOption("Mitochondria");
        question.setSampleAnswer("Mitochondria");
        when(questionService.getStudentQuestionsByExamId(EXAM_ID)).thenReturn(List.of(question));

        when(aiChatService.chat(any(), any(), any()))
                .thenReturn(ChatResponse.builder().answered(true).answer("The cell powerhouse is a common exam topic.").build());

        mockMvc.perform(post("/api/chat")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Is this statement true?\",\"examId\":5,\"questionId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answered").value(true));

        verify(questionService).getStudentQuestionsByExamId(EXAM_ID);
    }
}
