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

import java.util.List;
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
     * Finds a dialog by its database ID.
     */
    public Optional<Dialog> findDialogById(Long id) {
        return dialogRepository.findById(id);
    }

    /**
     * Finds all dialogs in the database.
     */
    public List<Dialog> findAllDialogs() {
        return dialogRepository.findAll();
    }

    /**
     * Handles sending a manual message by a human representative.
     * This automatically transitions the dialog's state to PAUSED.
     */
    public Message sendManualMessage(Long dialogId, String text) {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with id: " + dialogId));

        dialog.setAiState(AiState.PAUSED);
        dialogRepository.save(dialog);

        Message message = new Message(dialog, text, SenderType.HUMAN_REPRESENTATIVE);
        return messageRepository.save(message);
    }

    /**
     * Handles receiving a lead reply (USER).
     * If AI is ACTIVE, we trigger/simulate a response.
     * If AI is PAUSED, we ignore it (no simulated response).
     */
    public Message receiveLeadMessage(Long dialogId, String text) {
        Dialog dialog = dialogRepository.findById(dialogId)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found with id: " + dialogId));

        Message leadMessage = new Message(dialog, text, SenderType.USER);
        Message savedLeadMessage = messageRepository.save(leadMessage);

        if (dialog.getAiState() == AiState.ACTIVE) {
            Message aiMessage = new Message(dialog, "AI Automated Response to: " + text, SenderType.AI);
            messageRepository.save(aiMessage);
        }

        return savedLeadMessage;
    }
}
