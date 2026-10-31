package com.skillcheckr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.skillcheckr.config.AiChatProperties;
import com.skillcheckr.model.ChatCompletionResponse;

@Component
public class OpenAiChatClient implements AiChatClient {

    private final AiChatProperties aiChatProperties;
    private final RestClient.Builder restClientBuilder;

    public OpenAiChatClient(AiChatProperties aiChatProperties, RestClient.Builder restClientBuilder) {
        this.aiChatProperties = aiChatProperties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (aiChatProperties.getOpenaiApiKey() == null || aiChatProperties.getOpenaiApiKey().isBlank()) {
            throw new IllegalStateException("The AI assistant is not configured on this server.");
        }
        ChatCompletionResponse completion = restClientBuilder.baseUrl(aiChatProperties.getOpenaiBaseUrl())
                .build()
                .post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiChatProperties.getOpenaiApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatCompletionRequest(systemPrompt, userPrompt))
                .retrieve()
                .body(ChatCompletionResponse.class);
        if (completion == null || completion.getChoices() == null || completion.getChoices().isEmpty()) {
            throw new IllegalStateException("The AI assistant returned an empty response.");
        }
        ChatCompletionResponse.Choice choice = completion.getChoices().get(0);
        if (choice == null || choice.getMessage() == null || choice.getMessage().getContent() == null) {
            throw new IllegalStateException("The AI assistant returned an empty answer.");
        }
        return choice.getMessage().getContent().trim();
    }

    private Map<String, Object> chatCompletionRequest(String systemPrompt, String userPrompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", aiChatProperties.getOpenaiModel());
        request.put("max_tokens", aiChatProperties.getOpenaiMaxTokens());
        request.put("temperature", aiChatProperties.getOpenaiTemperature());
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(systemMessage);
        messages.add(userMessage);
        request.put("messages", messages);
        return request;
    }
}
