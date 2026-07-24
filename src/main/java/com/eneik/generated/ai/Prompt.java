package com.eneik.generated.ai;

public class Prompt {
    private final String systemPrompt;
    private final String userMessage;

    public Prompt(String systemPrompt, String userMessage) {
        this.systemPrompt = systemPrompt;
        this.userMessage = userMessage;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
