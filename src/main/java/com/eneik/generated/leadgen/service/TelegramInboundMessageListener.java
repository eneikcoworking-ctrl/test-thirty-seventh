package com.eneik.generated.leadgen.service;

import com.eneik.generated.leadgen.event.InboundMessageEvent;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TelegramInboundMessageListener {

    private static final Logger log = LoggerFactory.getLogger(TelegramInboundMessageListener.class);

    private final InboxService inboxService;
    private final ConversationRepository conversationRepository;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TelegramInboundMessageListener(InboxService inboxService, ConversationRepository conversationRepository) {
        this.inboxService = inboxService;
        this.conversationRepository = conversationRepository;
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        log.info("Shutting down TelegramInboundMessageListener executor pool...");
        executorService.shutdown();
    }

    @EventListener
    public void onInboundMessage(InboundMessageEvent event) {
        log.info("Event listener caught inbound message from chat: {}", event.getTelegramChatId());

        // Submit to the background worker pool to simulate real-time background evaluation
        executorService.submit(() -> {
            try {
                log.info("Background thread processing inbound message for chat ID: {}", event.getTelegramChatId());

                // Find or create conversation through service layer to ensure cache eviction/invalidation on creation
                Conversation conversation = inboxService.getOrCreateConversation(
                        event.getTelegramChatId(),
                        event.getLeadName(),
                        event.getLeadUsername(),
                        event.getLeadPhone()
                );

                // Trigger the AI evaluation engine
                log.info("Triggering AI evaluation engine for conversation ID: {}", conversation.getId());
                inboxService.receiveLeadMessage(conversation.getId(), event.getText());
                log.info("AI evaluation successfully completed for conversation ID: {}", conversation.getId());
            } catch (Exception e) {
                log.error("Error processing inbound message in background: " + e.getMessage(), e);
            }
        });
    }
}
