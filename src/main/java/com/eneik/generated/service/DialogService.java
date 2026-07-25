package com.eneik.generated.service;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = "dialogs", allEntries = true)
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
    @CacheEvict(value = "dialogs", allEntries = true)
    public Dialog handleStopTrigger(String telegramChatId, AiState newAiState) {
        Dialog dialog = dialogRepository.findByTelegramChatId(telegramChatId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with chat id: " + telegramChatId));

        dialog.setAiState(newAiState);
        Dialog saved = dialogRepository.save(dialog);

        return saved;
    }

    /**
     * Finds a dialog by its unique database identifier.
     */
    @Cacheable(value = "dialogs", key = "'id_' + #id")
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
    @Cacheable(value = "dialogs", key = "'page_' + (#pageable != null ? #pageable.pageNumber : 0) + '_size_' + (#pageable != null ? #pageable.pageSize : 50)")
    public Page<Dialog> findAllDialogs(Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 50);
        } else if (pageable.getPageSize() > 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
        }
        return dialogRepository.findAll(pageable);
    }
}
