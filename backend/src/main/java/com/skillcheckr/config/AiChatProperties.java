package com.skillcheckr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI assistant settings for the exam support chatbot. The key is supplied through the
 * environment so no credential is ever committed; when it is missing the assistant stays
 * unavailable and the API/UI degrade gracefully instead of failing.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

    private String openaiApiKey = "";

    private String openaiBaseUrl = "https://opencode.ai/inference/openai/v1";

    private String openaiModel = "glm-5.3";

    private int openaiMaxTokens = 800;

    private double openaiTemperature = 0.2;

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public void setOpenaiModel(String openaiModel) {
        this.openaiModel = openaiModel;
    }

    public int getOpenaiMaxTokens() {
        return openaiMaxTokens;
    }

    public void setOpenaiMaxTokens(int openaiMaxTokens) {
        this.openaiMaxTokens = openaiMaxTokens;
    }

    public double getOpenaiTemperature() {
        return openaiTemperature;
    }

    public void setOpenaiTemperature(double openaiTemperature) {
        this.openaiTemperature = openaiTemperature;
    }
}
