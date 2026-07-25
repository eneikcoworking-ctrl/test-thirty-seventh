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

    // Named Constants
    private static final String MEDIA_TYPE_TEXT = "text";
    private static final int MAX_TURN_LIMIT = 8;
    private static final String AI_RESPONSE_PREFIX = "AI Automated Response to: ";

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

        if (isPayloadInvalid(text, mediaType)) {
            return;
        }

        log.info("Processing inbound message on chat {}: '{}' (media: {})", telegramChatId, text, mediaType);

        boolean processedCrm = processCrmConversation(telegramChatId, text);
        boolean processedOutbound = processOutboundDialog(telegramChatId, text);

        if (!processedCrm && !processedOutbound) {
            log.info("No active Conversation or Dialog found in database for Telegram Chat ID: {}", telegramChatId);
        }
    }

    private boolean isPayloadInvalid(String text, String mediaType) {
        boolean isEmpty = (text == null || text.trim().isEmpty());
        boolean isUnsupportedMedia = (mediaType != null && !mediaType.equalsIgnoreCase(MEDIA_TYPE_TEXT) && !mediaType.isEmpty());

        if (isEmpty || isUnsupportedMedia) {
            log.error("Failed to process inbound message: {} without crashing.",
                    isEmpty ? "empty payload" : "unsupported media type '" + mediaType + "'");
            return true;
        }
        return false;
    }

    private boolean processCrmConversation(Long telegramChatId, String text) {
        Optional<Conversation> convOpt = conversationRepository.findByTelegramChatId(telegramChatId);
        if (convOpt.isEmpty()) {
            return false;
        }
        Conversation conversation = convOpt.get();
        log.info("Found CRM Conversation for chat {}. Triggering InboxService response flow.", telegramChatId);
        inboxService.receiveLeadMessage(conversation.getId(), text);
        return true;
    }

    private boolean processOutboundDialog(Long telegramChatId, String text) {
        Optional<Dialog> dialogOpt = dialogRepository.findByTelegramChatId(telegramChatId.toString());
        if (dialogOpt.isEmpty()) {
            return false;
        }
        Dialog dialog = dialogOpt.get();
        if (dialog.getAiState() != AiState.ACTIVE) {
            log.info("Outbound Dialog found but AI state is not ACTIVE (current state: {}). No AI turn triggered.", dialog.getAiState());
            saveInboundMessageGracefully(telegramChatId, text);
            return true;
        }

        log.info("Found active outbound Dialog for chat {}. Recording user response and triggering AI next turn.", telegramChatId);
        handleActiveDialogMessage(dialog, telegramChatId, text);
        return true;
    }

    private void saveInboundMessageGracefully(Long telegramChatId, String text) {
        try {
            dialogService.receiveInboundMessage(telegramChatId.toString(), text, SenderType.USER);
        } catch (Exception e) {
            log.error("Failed to save inbound message for stopped dialog: {}", e.getMessage());
        }
    }

    private void handleActiveDialogMessage(Dialog dialog, Long telegramChatId, String text) {
        try {
            dialogService.receiveInboundMessage(telegramChatId.toString(), text, SenderType.USER);
            triggerAiAutomatedResponse(dialog, telegramChatId, text);
        } catch (Exception e) {
            log.error("Failed to process outbound dialog inbound message gracefully: {}", e.getMessage());
        }
    }

    private void triggerAiAutomatedResponse(Dialog dialog, Long telegramChatId, String text) {
        // Generate automated AI response (triggers AI evaluation engine)
        Message aiResponse = new Message(dialog, AI_RESPONSE_PREFIX + text, SenderType.AI);
        messageRepository.save(aiResponse);

        // Re-evaluate count to check if we hit/exceeded the limit of 8
        long updatedCount = messageRepository.countByDialogId(dialog.getId());
        if (updatedCount >= MAX_TURN_LIMIT) {
            // Perform atomic database update to change AI state to STOPPED
            int updatedRows = dialogRepository.updateAiStateGuarded(dialog.getId(), AiState.STOPPED, AiState.ACTIVE);
            if (updatedRows > 0) {
                log.info("Dialog for chat {} hit turn limit. AI State set to STOPPED atomically.", telegramChatId);
            } else {
                log.warn("Failed to update AI State to STOPPED atomically (state was concurrently changed).");
            }
        }
    }
}
