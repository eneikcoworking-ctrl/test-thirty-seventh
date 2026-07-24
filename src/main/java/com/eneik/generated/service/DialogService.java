package com.eneik.generated.service;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DialogService {

    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public DialogService(DialogRepository dialogRepository, MessageRepository messageRepository) {
        this.dialogRepository = dialogRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Receives an inbound message, links it to an existing dialog (or creates a new dialog),
     * and stores it in the Messages table with the sender type.
     * If the sender is a USER and the message contains a stop-trigger, transitions the dialogue state
     * to AWAITING_HUMAN_INTERVENTION.
     */
    public Message receiveInboundMessage(String telegramChatId, String text, SenderType senderType) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    Dialog newDialog = new Dialog(telegramChatId, AiState.ACTIVE);
                    return dialogRepository.save(newDialog);
                });

        if (senderType == SenderType.USER && containsStopTrigger(text)) {
            dialog.setAiState(AiState.AWAITING_HUMAN_INTERVENTION);
            dialog = dialogRepository.save(dialog);
        }

        Message message = new Message(dialog, text, senderType);
        return messageRepository.save(message);
    }

    /**
     * Checks if a user message contains any stop-triggers.
     */
    public boolean containsStopTrigger(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase().trim();
        return lower.contains("stop") ||
               lower.contains("unsubscribe") ||
               lower.contains("human") ||
               lower.contains("agent") ||
               lower.contains("operator") ||
               lower.contains("halt");
    }

    /**
     * Checks if automated outreach or automated replies are allowed for the dialog.
     * Automated replies are only allowed when the AI state is ACTIVE.
     */
    public boolean isAutomatedReplyAllowed(String telegramChatId) {
        return dialogRepository.findByTelegramChatId(telegramChatId)
                .map(dialog -> dialog.getAiState() == AiState.ACTIVE)
                .orElse(false);
    }

    /**
     * Generates an automated AI reply if automated replies are allowed.
     * Returns the saved message if a reply was sent, or null if replies/outreach are halted.
     */
    public Message generateAutomatedReply(String telegramChatId, String replyText) {
        if (!isAutomatedReplyAllowed(telegramChatId)) {
            return null; // Automated outreach/replies are halted
        }
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with chat id: " + telegramChatId));

        Message replyMessage = new Message(dialog, replyText, SenderType.AI);
        return messageRepository.save(replyMessage);
    }

    /**
     * Handles a stop-trigger on a dialog by updating its AI state in the database.
     */
    public Dialog handleStopTrigger(String telegramChatId, AiState newAiState) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with chat id: " + telegramChatId));

        dialog.setAiState(newAiState);
        return dialogRepository.save(dialog);
    }
}
