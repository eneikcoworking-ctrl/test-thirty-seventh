package com.eneik.generated.service;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

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
     */
    public Message receiveInboundMessage(String telegramChatId, String text, SenderType senderType) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    Dialog newDialog = new Dialog(telegramChatId, AiState.ACTIVE);
                    return dialogRepository.save(newDialog);
                });

        Message message = new Message(dialog, text, senderType);
        return messageRepository.save(message);
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

    /**
     * Processes an inbound message, persists it, and returns the most recent dialog context
     * up to a specified maximum number of messages. If the total number of messages reaches
     * 16 (8 turns), the session is stopped and an IllegalStateException is thrown.
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public List<Message> processInboundMessageAndGetContext(String telegramChatId, String text, SenderType senderType, int maxMessages) {
        // Persist the incoming message
        Message newMessage = receiveInboundMessage(telegramChatId, text, senderType);
        Dialog dialog = newMessage.getDialog();

        // Check total turns
        long totalMessages = messageRepository.countByDialogId(dialog.getId());
        if (totalMessages >= 16) {
            handleStopTrigger(telegramChatId, AiState.STOPPED);
            throw new IllegalStateException("Dialog reached the maximum allowed turns (8 turns / 16 messages). AI state set to STOPPED.");
        }

        // Fetch context history truncating to maxMessages
        List<Message> contextHistory = messageRepository.findByDialogIdOrderByReceivedAtDesc(dialog.getId(), PageRequest.of(0, maxMessages));

        // Reverse so that context is in chronological order
        List<Message> mutableHistory = new java.util.ArrayList<>(contextHistory);
        Collections.reverse(mutableHistory);
        return mutableHistory;
    }
}
