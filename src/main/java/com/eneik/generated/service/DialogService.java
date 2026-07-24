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
     * Enforces an 8 back-and-forth message limit as a concrete blocker.
     */
    public Message receiveInboundMessage(String telegramChatId, String text, SenderType senderType) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    Dialog newDialog = new Dialog(telegramChatId, AiState.ACTIVE);
                    return dialogRepository.save(newDialog);
                });

        // 1. Check existing count of messages in this dialogue session
        long currentCount = messageRepository.countByDialogId(dialog.getId());
        if (currentCount >= 8) {
            dialog.setAiState(AiState.STOPPED);
            dialogRepository.save(dialog);
            throw new IllegalStateException("Conversation limit reached: back-and-forth message count exceeds 8.");
        }

        // 2. Save the new message
        Message message = new Message(dialog, text, senderType);
        Message savedMessage = messageRepository.save(message);

        // 3. Re-evaluate count to check if we just hit/exceeded the limit of 8
        long updatedCount = messageRepository.countByDialogId(dialog.getId());
        if (updatedCount >= 8) {
            dialog.setAiState(AiState.STOPPED);
            dialogRepository.save(dialog);
        }

        return savedMessage;
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
