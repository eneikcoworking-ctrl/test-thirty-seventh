package com.eneik.generated.leadgen.service;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.Message;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.repository.MessageRepository;
import com.eneik.generated.leadgen.repository.TelegramAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class InboxService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TelegramAccountRepository telegramAccountRepository;
    private final TelegramBridgeService telegramBridgeService;

    public InboxService(ConversationRepository conversationRepository,
                        MessageRepository messageRepository,
                        TelegramAccountRepository telegramAccountRepository,
                        TelegramBridgeService telegramBridgeService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.telegramAccountRepository = telegramAccountRepository;
        this.telegramBridgeService = telegramBridgeService;
    }

    /**
     * Retrieves all active/historical conversations/chats from all accounts.
     */
    @Transactional(readOnly = true)
    public List<Conversation> getAllConversations() {
        return conversationRepository.findAll();
    }

    /**
     * Dispatches a manual message via the Telegram layer, updating history and conversations.
     */
    @Transactional
    public Message dispatchManualMessage(String telegramAccountId, String leadId, String content) {
        // 1. Dispatch message via the TDLib/Telegram layer simulated bridge
        String tgMessageId = telegramBridgeService.dispatchMessage(telegramAccountId, leadId, content);

        OffsetDateTime now = OffsetDateTime.now();

        // 2. Persist message in the relational database history
        Message message = new Message();
        message.setTelegramAccountId(telegramAccountId);
        message.setLeadId(leadId);
        message.setContent(content);
        message.setDirection("OUTBOUND");
        message.setStatus("SENT");
        message.setTimestamp(now);
        Message savedMessage = messageRepository.save(message);

        // 3. Update or create the Conversation to track the latest turn
        Conversation conversation = conversationRepository
                .findByTelegramAccountIdAndLeadId(telegramAccountId, leadId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setTelegramAccountId(telegramAccountId);
                    newConv.setLeadId(leadId);
                    // Generate a placeholder username or rely on existing lead entity if needed
                    newConv.setLeadUsername("lead_" + leadId);
                    return newConv;
                });

        conversation.setLastMessage(content);
        conversation.setLastMessageTimestamp(now);
        conversationRepository.save(conversation);

        return savedMessage;
    }
}
