package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.InboxService;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TelegramBackgroundEventListener {

    private static final Logger log = LoggerFactory.getLogger(TelegramBackgroundEventListener.class);
    private static final int MAX_TURN_LIMIT = 8;
    private static final String MEDIA_TYPE_TEXT = "text";

    private final TgAccountRepository tgAccountRepository;
    private final DialogRepository dialogRepository;
    private final DialogService dialogService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final InboxService inboxService;

    public TelegramBackgroundEventListener(TgAccountRepository tgAccountRepository,
                                           DialogRepository dialogRepository,
                                           DialogService dialogService,
                                           MessageRepository messageRepository,
                                           ConversationRepository conversationRepository,
                                           InboxService inboxService) {
        this.tgAccountRepository = tgAccountRepository;
        this.dialogRepository = dialogRepository;
        this.dialogService = dialogService;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.inboxService = inboxService;
    }

    @Transactional
    public void handleAccountStatusUpdate(String phoneNumber, String newStatus) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || newStatus == null || newStatus.trim().isEmpty()) {
            log.warn("Skipping account status update: missing phoneNumber or status");
            return;
        }

        log.info("Received background account status update for phone {}: setting status to '{}'", phoneNumber, newStatus);
        int updatedCount = tgAccountRepository.updateStatusByPhoneNumber(phoneNumber, newStatus, LocalDateTime.now());
        if (updatedCount == 0) {
            log.warn("No account found with phone number {} to update status.", phoneNumber);
        } else {
            log.info("Successfully updated status of account with phone {} to '{}' in database.", phoneNumber, newStatus);
        }
    }

    @Transactional
    public void handleInboundMessage(Long telegramChatId, String text, String mediaType) {
        if (telegramChatId == null) {
            log.warn("Inbound message received with null telegramChatId. Skipping processing.");
            return;
        }

        if (isInvalidPayload(text, mediaType)) {
            log.error("Failed to process inbound message on chat {}: invalid payload.", telegramChatId);
            return;
        }

        log.info("Processing inbound message on chat {}: '{}' (media: {})", telegramChatId, text, mediaType);

        boolean processedConv = processCrmConversation(telegramChatId, text);
        boolean processedDialog = processOutboundDialog(telegramChatId, text);

        if (!processedConv && !processedDialog) {
            log.info("No active Conversation or Dialog found in database for Telegram Chat ID: {}", telegramChatId);
        }
    }

    private boolean isInvalidPayload(String text, String mediaType) {
        boolean isEmpty = (text == null || text.trim().isEmpty());
        boolean isUnsupportedMedia = (mediaType != null && !mediaType.equalsIgnoreCase(MEDIA_TYPE_TEXT) && !mediaType.isEmpty());
        return isEmpty || isUnsupportedMedia;
    }

    private boolean processCrmConversation(Long telegramChatId, String text) {
        Optional<Conversation> convOpt = conversationRepository.findByTelegramChatId(telegramChatId);
        if (convOpt.isEmpty()) {
            return false;
        }
        log.info("Found CRM Conversation for chat {}. Triggering InboxService response flow.", telegramChatId);
        inboxService.receiveLeadMessage(convOpt.get().getId(), text);
        return true;
    }

    private boolean processOutboundDialog(Long telegramChatId, String text) {
        Optional<Dialog> dialogOpt = dialogRepository.findByTelegramChatId(telegramChatId.toString());
        if (dialogOpt.isEmpty()) {
            return false;
        }

        Dialog dialog = dialogOpt.get();
        if (dialog.getAiState() != AiState.ACTIVE) {
            return processInactiveDialog(telegramChatId, text, dialog);
        }
        return processActiveDialog(telegramChatId, text);
    }

    private boolean processInactiveDialog(Long telegramChatId, String text, Dialog dialog) {
        log.info("Outbound Dialog for chat {} found but AI state is not ACTIVE (current state: {}). No AI turn triggered.",
                telegramChatId, dialog.getAiState());
        try {
            dialogService.receiveInboundMessage(telegramChatId.toString(), text, SenderType.USER);
            return true;
        } catch (Exception e) {
            log.error("Failed to save inbound message for stopped dialog: {}", e.getMessage());
            return false;
        }
    }

    private boolean processActiveDialog(Long telegramChatId, String text) {
        log.info("Found active outbound Dialog for chat {}. Recording user response and triggering AI next turn.", telegramChatId);
        try {
            dialogService.receiveInboundMessage(telegramChatId.toString(), text, SenderType.USER);
            executeAiEvaluationTurn(telegramChatId, text);
            return true;
        } catch (Exception e) {
            log.error("Failed to process outbound dialog inbound message gracefully: {}", e.getMessage());
            return false;
        }
    }

    private void executeAiEvaluationTurn(Long telegramChatId, String text) {
        Optional<Dialog> updatedDialogOpt = dialogRepository.findByTelegramChatId(telegramChatId.toString());
        if (updatedDialogOpt.isEmpty() || updatedDialogOpt.get().getAiState() != AiState.ACTIVE) {
            return;
        }

        Dialog updatedDialog = updatedDialogOpt.get();
        Message aiResponse = new Message(updatedDialog, "AI Automated Response to: " + text, SenderType.AI);
        messageRepository.save(aiResponse);

        long updatedCount = messageRepository.countByDialogId(updatedDialog.getId());
        if (updatedCount >= MAX_TURN_LIMIT) {
            int updatedRows = dialogRepository.updateAiStateGuarded(updatedDialog.getId(), AiState.ACTIVE, AiState.STOPPED);
            if (updatedRows > 0) {
                log.info("Dialog for chat {} hit turn limit. AI State set to STOPPED.", telegramChatId);
            } else {
                log.warn("Failed to update AI State to STOPPED for Dialog {} due to concurrent modification.", updatedDialog.getId());
            }
        }
    }
}
