package com.eneik.generated.service;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class DialogService {

    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;

    private static final long MAX_MESSAGES_LIMIT = 8;
    private static final String AI_RESPONSE_PREFIX = "AI Automated Response to: ";

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

        long currentCount = messageRepository.countByDialogId(dialog.getId());
        if (currentCount >= MAX_MESSAGES_LIMIT) {
            dialogRepository.updateAiStateGuarded(dialog.getId(), AiState.STOPPED, AiState.ACTIVE);
            throw new IllegalStateException("Conversation limit reached: back-and-forth message count exceeds " + MAX_MESSAGES_LIMIT + ".");
        }

        Message message = new Message(dialog, text, senderType);
        Message savedMessage = messageRepository.save(message);

        long updatedCount = messageRepository.countByDialogId(dialog.getId());
        if (updatedCount >= MAX_MESSAGES_LIMIT) {
            dialogRepository.updateAiStateGuarded(dialog.getId(), AiState.STOPPED, AiState.ACTIVE);
        }

        return savedMessage;
    }

    /**
     * Handles a stop-trigger on a dialog by updating its AI state in the database.
     */
    public Dialog handleStopTrigger(String telegramChatId, AiState newAiState) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with chat id: " + telegramChatId));

        int updatedCount = dialogRepository.updateAiStateGuarded(dialog.getId(), newAiState, dialog.getAiState());
        if (updatedCount == 0 && dialog.getAiState() != newAiState) {
            throw new IllegalStateException("Failed to update AI state due to concurrent modification.");
        }
        return dialogRepository.findById(dialog.getId()).get();
    }

    /**
     * Finds a dialog by its unique database identifier.
     */
    public Optional<Dialog> findDialogById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return dialogRepository.findById(id);
    }

    /**
     * Retrieves dialogs with pagination to prevent out-of-memory errors as the database grows.
     * Enforces a maximum page size constraint of 50.
     */
    public Page<Dialog> findAllDialogs(Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 50);
        } else if (pageable.getPageSize() > 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
        }
        return dialogRepository.findAll(pageable);
    }

    public void generateAiResponse(Long dialogId, String userText) {
        Dialog dialog = dialogRepository.findById(dialogId).orElseThrow(() -> new IllegalArgumentException("Dialog not found"));
        if (dialog.getAiState() != AiState.ACTIVE) return;

        Message aiResponse = new Message(dialog, AI_RESPONSE_PREFIX + userText, SenderType.AI);
        messageRepository.save(aiResponse);
        if (messageRepository.countByDialogId(dialogId) >= MAX_MESSAGES_LIMIT) {
            dialogRepository.updateAiStateGuarded(dialogId, AiState.STOPPED, AiState.ACTIVE);
        }
    }
}
