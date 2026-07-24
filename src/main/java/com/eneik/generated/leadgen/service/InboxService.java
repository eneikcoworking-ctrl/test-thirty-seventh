package com.eneik.generated.leadgen.service;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InboxService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final TelegramBridgeService telegramBridgeService;

    public InboxService(ConversationRepository conversationRepository,
                        ConversationMessageRepository conversationMessageRepository,
                        TelegramBridgeService telegramBridgeService) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.telegramBridgeService = telegramBridgeService;
    }

    @Transactional(readOnly = true)
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
    public List<ConversationMessage> getMessages(String conversationId, int limit, String beforeMessageId) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "sentAt"));
        if (beforeMessageId != null && !beforeMessageId.trim().isEmpty()) {
            return conversationMessageRepository.findByConversationIdAndIdLessThan(conversationId, beforeMessageId, pageable);
        } else {
            return conversationMessageRepository.findByConversationId(conversationId, pageable);
        }
    }

    @Transactional
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
        conversation.setStatus("ACTIVE");
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        return savedMessage;
    }
}
