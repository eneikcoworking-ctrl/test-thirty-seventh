package com.eneik.generated.ai;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Locale;

@Component
public class FallbackChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String systemPrompt = prompt.getSystemPrompt();
        String userMessage = prompt.getUserMessage();

        String responseText = generateAdherentResponse(userMessage, systemPrompt);
        Generation generation = new Generation(responseText);
        return new ChatResponse(Collections.singletonList(generation));
    }

    private String generateAdherentResponse(String userMessage, String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return "Echo: " + userMessage;
        }

        String prefix = "";
        String suffix = "";
        String tone = "";

        String[] lines = systemPrompt.split("\\r?\\n");
        boolean structuredPrompt = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase(Locale.ROOT).startsWith("PREFIX:")) {
                prefix = trimmed.substring(7).trim();
                structuredPrompt = true;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("SUFFIX:")) {
                suffix = trimmed.substring(7).trim();
                structuredPrompt = true;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("TONE:")) {
                tone = trimmed.substring(5).trim().toUpperCase(Locale.ROOT);
                structuredPrompt = true;
            }
        }

        String coreResponse = userMessage;
        if (structuredPrompt) {
            if ("UPPERCASE".equals(tone)) {
                coreResponse = coreResponse.toUpperCase(Locale.ROOT);
            } else if ("LOWERCASE".equals(tone)) {
                coreResponse = coreResponse.toLowerCase(Locale.ROOT);
            } else if ("POLITE".equals(tone)) {
                coreResponse = "Dear client, " + coreResponse;
            } else if ("REVERSE".equals(tone)) {
                coreResponse = new StringBuilder(coreResponse).reverse().toString();
            }

            StringBuilder sb = new StringBuilder();
            if (!prefix.isEmpty()) {
                sb.append(prefix).append(" ");
            }
            sb.append(coreResponse);
            if (!suffix.isEmpty()) {
                sb.append(" ").append(suffix);
            }
            return sb.toString();
        } else {
            return "Adhering to: " + systemPrompt + " -> Response to '" + userMessage + "'";
        }
    }
}
