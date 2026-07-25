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
    private static final String SUPPORTED_MEDIA_TYPE_TEXT = "text";

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

    /**
     * Detects account status updates from Telegram and updates the account status in the database atomically.
     */
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

    /**
     * Background event listener that detects new inbound messages from leads and triggers the AI evaluation engine.
     * Handles empty payloads and unsupported media gracefully without crashing.
     */
    @Transactional
    public void handleInboundMessage(Long telegramChatId, String text, String mediaType) {
        if (telegramChatId == null) {
            log.warn("Inbound message received with null telegramChatId. Skipping processing.");
            return;
        }

        // Detect empty payload or unsupported media gracefully
        boolean isEmpty = (text == null || text.trim().isEmpty());
        boolean isUnsupportedMedia = (mediaType != null && !mediaType.equalsIgnoreCase(SUPPORTED_MEDIA_TYPE_TEXT) && !mediaType.isEmpty());

        if (isEmpty || isUnsupportedMedia) {
            log.error("Failed to process inbound message on chat {}: {} without crashing.",
                    telegramChatId,
                    isEmpty ? "empty payload" : "unsupported media type '" + mediaType + "'");
            return;
        }

        log.info("Processing inbound message on chat {}: '{}' (media: {})", telegramChatId, text, mediaType);

        boolean processedAny = false;

        // 1. Process for CRM Live Chat (Conversation)
        processedAny |= processCrmConversation(telegramChatId, text);

        // 2. Process for Outbound Dialogue Flow (Dialog)
        processedAny |= processOutboundDialog(telegramChatId, text);

        if (!processedAny) {
            log.info("No active Conversation or Dialog found in database for Telegram Chat ID: {}", telegramChatId);
        }
    }

    private boolean processCrmConversation(Long telegramChatId, String text) {
        return conversationRepository.findByTelegramChatId(telegramChatId).map(conversation -> {
            log.info("Found CRM Conversation for chat {}. Triggering InboxService response flow.", telegramChatId);
            inboxService.receiveLeadMessage(conversation.getId(), text);
            return true;
        }).orElse(false);
    }

    private boolean processOutboundDialog(Long telegramChatId, String text) {
        Optional<Dialog> dialogOpt = dialogRepository.findByTelegramChatId(telegramChatId.toString());
        if (!dialogOpt.isPresent()) return false;

        Dialog dialog = dialogOpt.get();
        try {
            dialogService.receiveInboundMessage(telegramChatId.toString(), text, SenderType.USER);
            if (dialog.getAiState() == AiState.ACTIVE) {
                dialogService.generateAiResponse(dialog.getId(), text);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to process outbound dialog inbound message gracefully: {}", e.getMessage());
            return false;
        }
    }
}
