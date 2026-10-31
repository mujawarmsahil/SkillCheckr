package com.skillcheckr.service;

import com.skillcheckr.model.ChatContext;
import com.skillcheckr.model.ChatRequest;
import com.skillcheckr.model.ChatResponse;

public interface AiChatService {

    ChatResponse chat(ChatRequest request, ChatContext context, String callerRole);
}
