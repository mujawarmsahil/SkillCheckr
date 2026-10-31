package com.skillcheckr.service;

/**
 * Thin client for the configured AI provider. Keeping the provider call behind an interface
 * makes the assistant service unit-testable without standing up an HTTP server.
 */
public interface AiChatClient {

    String complete(String systemPrompt, String userPrompt);
}
