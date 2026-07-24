package com.eneik.generated.service;

import com.eneik.generated.ai.ChatModel;
import com.eneik.generated.ai.Prompt;
import com.eneik.generated.ai.ChatResponse;
import com.eneik.generated.model.*;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class AiDialogueService {

    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;
    private final ChatModel chatModel;

    @Autowired
    public AiDialogueService(DialogRepository dialogRepository, MessageRepository messageRepository, ChatModel chatModel) {
        this.dialogRepository = dialogRepository;
        this.messageRepository = messageRepository;
        this.chatModel = chatModel;
    }

    /**
     * Processes an incoming user message, evaluates the context, detects intent,
     * updates the dialog state if escalated, and generates the response adhering
     * to the configured persona/system prompt.
     */
    public AiEvaluationResult evaluateAndRespond(String telegramChatId, String userMessageText, String systemPrompt) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    Dialog newDialog = new Dialog(telegramChatId, AiState.ACTIVE);
                    return dialogRepository.save(newDialog);
                });

        // 1. Store the incoming user message
        Message userMessage = new Message(dialog, userMessageText, SenderType.USER);
        messageRepository.save(userMessage);

        // 2. Detect intent (booking request, human escalation, support escalation)
        String lowerMessage = userMessageText.toLowerCase(Locale.ROOT);
        boolean isEscalation = false;
        String detectedIntent = null;

        if (lowerMessage.contains("book") || lowerMessage.contains("reserve") || lowerMessage.contains("appointment")
                || lowerMessage.contains("schedule") || lowerMessage.contains("slot")) {
            isEscalation = true;
            detectedIntent = "BOOKING_REQUEST";
        } else if (lowerMessage.contains("human") || lowerMessage.contains("agent") || lowerMessage.contains("support")
                || lowerMessage.contains("escalate") || lowerMessage.contains("help")) {
            isEscalation = true;
            detectedIntent = "HUMAN_ESCALATION";
        }

        if (isEscalation) {
            // Update the dialog's AI state to STOPPED in the database
            dialog.setAiState(AiState.STOPPED);
            dialogRepository.save(dialog);

            // Generate an escalation response text
            String escalationResponse = "Escalating to a human agent for intent: " + detectedIntent;
            Message systemMsg = new Message(dialog, escalationResponse, SenderType.SYSTEM);
            messageRepository.save(systemMsg);

            return new AiEvaluationResult(escalationResponse, true, detectedIntent);
        }

        // 3. Generate response adhering to instructions using our Spring AI styled ChatModel
        Prompt prompt = new Prompt(systemPrompt, userMessageText);
        ChatResponse chatResponse = chatModel.call(prompt);
        String responseText = (chatResponse != null && chatResponse.getResult() != null)
                ? chatResponse.getResult().getText()
                : "Echo: " + userMessageText;

        // Store the AI's generated response
        Message aiMessage = new Message(dialog, responseText, SenderType.AI);
        messageRepository.save(aiMessage);

        return new AiEvaluationResult(responseText, false, null);
    }
}
