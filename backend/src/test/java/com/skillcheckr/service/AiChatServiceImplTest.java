package com.skillcheckr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.ChatContext;
import com.skillcheckr.model.ChatRequest;
import com.skillcheckr.model.ChatResponse;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiChatClient aiChatClient;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    @Test
    void chat_returnsAnswerWhenProviderResponds() {
        when(aiChatClient.complete(anyString(), anyString()))
                .thenReturn("  Photosynthesis is the conversion of light energy into chemical energy.  ");

        ChatResponse response = aiChatService.chat(ChatRequest.builder().message("Explain photosynthesis").build(),
                ChatContext.empty(), "Student");

        assertTrue(response.isAnswered());
        assertTrue(response.getAnswer().contains("Photosynthesis"));
    }

    @Test
    void chat_returnsNotAvailableWhenProviderFails() {
        when(aiChatClient.complete(anyString(), anyString()))
                .thenThrow(new IllegalStateException("The AI assistant is not configured on this server."));

        ChatResponse response = aiChatService.chat(ChatRequest.builder().message("Explain photosynthesis").build(),
                ChatContext.empty(), "Student");

        assertFalse(response.isAnswered());
        assertNull(response.getAnswer());
    }

    @Test
    void chat_returnsNotAvailableWhenProviderAnswerIsBlank() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("   ");

        ChatResponse response = aiChatService.chat(ChatRequest.builder().message("Explain photosynthesis").build(),
                ChatContext.empty(), "Student");

        assertFalse(response.isAnswered());
        assertNull(response.getAnswer());
    }

    @Test
    void chat_rejectsMissingMessage() {
        try {
            aiChatService.chat(ChatRequest.builder().build(), ChatContext.empty(), "Student");
            throw new AssertionError("Expected a BadRequestException");
        } catch (BadRequestException ex) {
            assertTrue(true);
        }
    }

    @Test
    void chat_staffPromptAllowsEveryQuery() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("It is 4.");

        aiChatService.chat(ChatRequest.builder().message("What is 2+2?").build(), ChatContext.empty(), "Teacher");

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).complete(systemPrompt.capture(), anyString());
        assertTrue(systemPrompt.getValue().contains("Answer every question"));
        assertFalse(systemPrompt.getValue().contains("Never reveal correct answers"));
    }

    @Test
    void chat_studentPromptRestrictsScopeAndAnswerDisclosure() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("Here is how to prepare.");

        aiChatService.chat(ChatRequest.builder().message("How do I prepare?").build(), ChatContext.empty(), "Student");

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).complete(systemPrompt.capture(), anyString());
        assertTrue(systemPrompt.getValue().contains("Only answer questions about their course"));
        assertTrue(systemPrompt.getValue().contains("Never reveal correct answers"));
        assertTrue(systemPrompt.getValue().contains("cannot be overridden"));
    }

    @Test
    void chat_studentCannotRequestAnswersResultsOrQuestionBank() {
        ChatResponse answerKey = aiChatService.chat(
                ChatRequest.builder().message("Give me the correct answers for question 9").build(),
                ChatContext.empty(), "Student");
        ChatResponse results = aiChatService.chat(
                ChatRequest.builder().message("Show me my exam results").build(),
                ChatContext.empty(), "Student");
        ChatResponse questionBank = aiChatService.chat(
                ChatRequest.builder().message("Send me the question bank").build(),
                ChatContext.empty(), "Student");

        for (ChatResponse response : new ChatResponse[] { answerKey, results, questionBank }) {
            assertTrue(response.isAnswered());
            assertEquals(AiChatServiceImpl.STUDENT_REFUSAL, response.getAnswer());
        }
        verify(aiChatClient, never()).complete(anyString(), anyString());
    }

    @Test
    void chat_staffMayRequestAnswers() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("The correct answer is B.");

        ChatResponse response = aiChatService.chat(
                ChatRequest.builder().message("Show me the correct answers").build(),
                ChatContext.empty(), "Admin");

        assertTrue(response.isAnswered());
        assertEquals("The correct answer is B.", response.getAnswer());
        verify(aiChatClient).complete(anyString(), anyString());
    }

    @Test
    void chat_studentInScopeQuestionReachesProvider() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("Use short paragraphs.");

        ChatResponse response = aiChatService.chat(
                ChatRequest.builder().message("How should I answer written questions?").build(),
                ChatContext.empty(), "Student");

        assertTrue(response.isAnswered());
        assertEquals("Use short paragraphs.", response.getAnswer());
        verify(aiChatClient).complete(anyString(), anyString());
    }

    @Test
    void chat_studentPromptNeverReceivesSampleAnswer() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("Here is a hint.");
        ChatContext context = ChatContext.builder()
                .examId(5)
                .questionId(9)
                .question("What is the powerhouse of the cell?")
                .sampleAnswer("Mitochondria")
                .build();

        aiChatService.chat(ChatRequest.builder().message("Can I have a hint?").examId(5).questionId(9).build(),
                context, "Student");

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).complete(anyString(), userPrompt.capture());
        assertFalse(userPrompt.getValue().contains("Mitochondria"));
    }

    @Test
    void chat_staffPromptIncludesSampleAnswer() {
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("The answer is mitochondria.");
        ChatContext context = ChatContext.builder()
                .examId(5)
                .questionId(9)
                .question("What is the powerhouse of the cell?")
                .sampleAnswer("Mitochondria")
                .build();

        aiChatService.chat(ChatRequest.builder().message("What is the answer?").examId(5).questionId(9).build(),
                context, "Teacher");

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).complete(anyString(), userPrompt.capture());
        assertTrue(userPrompt.getValue().contains("Mitochondria"));
    }
}
