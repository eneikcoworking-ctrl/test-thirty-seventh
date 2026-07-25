package com.eneik.generated.leadgen.service;

import com.eneik.generated.config.CacheConstants;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InboxService {

    private static final Logger log = LoggerFactory.getLogger(InboxService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final TelegramBridgeService telegramBridgeService;
    private final org.springframework.cache.CacheManager cacheManager;

    public InboxService(ConversationRepository conversationRepository,
                        ConversationMessageRepository conversationMessageRepository,
                        TelegramBridgeService telegramBridgeService,
                        org.springframework.cache.CacheManager cacheManager) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.telegramBridgeService = telegramBridgeService;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CONVERSATIONS)
    public Page<Conversation> getConversations(String status, String assignedAgentId, int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "lastMessageAt"));

        boolean hasStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL"));
        boolean hasAgent = (assignedAgentId != null && !assignedAgentId.trim().isEmpty());

        if (hasStatus && hasAgent) {
            return conversationRepository.findByStatusAndAssignedAgentId(status.toUpperCase(), assignedAgentId, pageable);
        } else if (hasStatus) {
            return conversationRepository.findByStatus(status.toUpperCase(), pageable);
        } else if (hasAgent) {
            return conversationRepository.findByAssignedAgentId(assignedAgentId, pageable);
        } else {
            return conversationRepository.findAll(pageable);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.MESSAGES, key = "#conversationId", condition = "#beforeMessageId == null")
    public List<ConversationMessage> getMessages(String conversationId, int limit, String beforeMessageId) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt"));
        if (beforeMessageId != null && !beforeMessageId.trim().isEmpty()) {
            return conversationMessageRepository.findByConversationIdAndIdLessThan(conversationId, beforeMessageId, pageable);
        } else {
            return conversationMessageRepository.findByConversationId(conversationId, pageable);
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.MESSAGES, key = "#conversationId"),
        @CacheEvict(value = CacheConstants.CONVERSATIONS, allEntries = true)
    })
    public ConversationMessage sendManualMessage(String conversationId, String text) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        // 1. Dispatch manual outreach message via the Telegram bridge layer (simulated/actual JNI/JNA wrapper)
        telegramBridgeService.dispatchMessage(conversation.getTelegramChatId(), text);

        OffsetDateTime now = OffsetDateTime.now();

        // 2. Persist the OUTBOUND message in the conversation message history
        ConversationMessage message = new ConversationMessage();
        message.setId(UUID.randomUUID().toString());
        message.setConversationId(conversationId);
        message.setText(text);
        message.setSenderType("HUMAN_REPRESENTATIVE");
        message.setSentAt(now);
        message.setSenderName("Human Agent");
        ConversationMessage savedMessage = conversationMessageRepository.save(message);

        // 3. Update the conversation state (mark as ESCALATED/ACTIVE, update last turn timestamp)
        // Manual message automatically marks active/handled status
        // Given an AI-active conversation, When a manual message is sent, Then the status updates to paused.
        conversation.setStatus("PAUSED");
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        evictConversationCaches();

        return savedMessage;
    }

    /**
     * Receives a lead message. If the conversation status is ACTIVE (AI is active),
     * an automated AI response is triggered. If the conversation status is PAUSED,
     * the lead reply is saved but the AI ignores it (no automated AI response is added).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.MESSAGES, key = "#conversationId"),
        @CacheEvict(value = CacheConstants.CONVERSATIONS, allEntries = true)
    })
    public ConversationMessage receiveLeadMessage(String conversationId, String text) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        OffsetDateTime now = OffsetDateTime.now();

        ConversationMessage leadMessage = new ConversationMessage();
        leadMessage.setId(UUID.randomUUID().toString());
        leadMessage.setConversationId(conversationId);
        leadMessage.setText(text);
        leadMessage.setSenderType("LEAD");
        leadMessage.setSentAt(now);
        leadMessage.setSenderName(conversation.getLeadName());
        ConversationMessage savedLeadMessage = conversationMessageRepository.save(leadMessage);

        // Given a paused dialogue, When a lead replies, Then the AI ignores the reply.
        if ("ACTIVE".equalsIgnoreCase(conversation.getStatus())) {
            boolean stopTriggered = false;

            if (text != null) {
                String lowerText = text.toLowerCase();
                if (lowerText.contains("stop") || lowerText.contains("unsubscribe") || lowerText.contains("human")) {
                    stopTriggered = true;
                }
            }

            if (!stopTriggered) {
                long aiTurns = conversationMessageRepository.countByConversationIdAndSenderType(conversationId, "AI_AGENT");
                if (aiTurns >= 5) {
                    stopTriggered = true;
                }
            }

            if (stopTriggered) {
                conversation.setStatus("ESCALATED");
            } else {
                ConversationMessage aiMessage = new ConversationMessage();
                aiMessage.setId(UUID.randomUUID().toString());
                aiMessage.setConversationId(conversationId);
                aiMessage.setText("AI Automated Response to: " + text);
                aiMessage.setSenderType("AI_AGENT");
                aiMessage.setSentAt(now.plusSeconds(1));
                aiMessage.setSenderName("AI Bot");
                conversationMessageRepository.save(aiMessage);
            }
        }

        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        evictConversationCaches();

        return savedLeadMessage;
    }

    public void evictConversationCaches() {
        if (cacheManager != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache(CacheConstants.CONVERSATIONS);
            if (cache != null) {
                cache.clear();
                log.info("Actively and immediately cleared conversations list cache programmatically.");
            }
        }
    }
}
