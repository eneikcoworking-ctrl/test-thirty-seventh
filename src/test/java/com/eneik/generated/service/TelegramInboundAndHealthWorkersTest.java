package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import com.eneik.generated.leadgen.service.TelegramInboundBackgroundWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TelegramInboundAndHealthWorkersTest {

    @Autowired
    private TelegramBridgeService telegramBridgeService;

    @Autowired
    private TelegramInboundBackgroundWorker inboundWorker;

    @Autowired
    private TelegramSessionHealthMonitorWorker healthMonitorWorker;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @BeforeEach
    public void setup() {
        telegramBridgeService.clearIncomingMessages();
        conversationMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        tgAccountRepository.deleteAll();
    }

    @Test
    public void testInboundBackgroundWorkerProcessesMessagesAndTriggersAi() throws Exception {
        // Given an active conversation with a lead
        Long telegramChatId = 987654321L;
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                telegramChatId,
                "Alice Green",
                "alice_green",
                "+1555555555",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // Queue a valid message in the bridge
        telegramBridgeService.queueIncomingMessage(
                telegramChatId,
                "Hello, I would like to get started!",
                "Alice Green",
                "alice_green",
                "+1555555555"
        );

        // When the background worker runs
        inboundWorker.checkForInboundMessages();

        // Then the message must be processed and AI response generated (asynchronously in background)
        boolean aiReplied = false;
        for (int i = 0; i < 20; i++) {
            List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(convId, null);
            if (messages.size() >= 2) {
                boolean hasLeadMsg = messages.stream().anyMatch(m -> "LEAD".equalsIgnoreCase(m.getSenderType()));
                boolean hasAiMsg = messages.stream().anyMatch(m -> "AI_AGENT".equalsIgnoreCase(m.getSenderType()));
                if (hasLeadMsg && hasAiMsg) {
                    aiReplied = true;
                    break;
                }
            }
            Thread.sleep(50);
        }

        assertThat(aiReplied).isTrue();
    }

    @Test
    public void testInboundBackgroundWorkerRemainsIdleWhenNoMessages() {
        // Given no messages are queued in the bridge
        telegramBridgeService.clearIncomingMessages();

        // When the background worker runs, it should execute immediately without error
        inboundWorker.checkForInboundMessages();

        // Confirm queue is still empty
        assertThat(telegramBridgeService.pollIncomingMessage()).isNull();
    }

    @Test
    public void testInboundBackgroundWorkerHandlesMalformedMessagesGracefully() {
        // Queue a malformed message (missing leadName) and then a valid one
        telegramBridgeService.queueIncomingMessage(111L, "Help", null, "user", "123");
        telegramBridgeService.queueIncomingMessage(222L, "Valid text", "Bob", "bob1", "456");

        // When the background worker runs
        inboundWorker.checkForInboundMessages();

        // Both messages should be cleared from the poll queue (first was processed/logged, second processed/published)
        assertThat(telegramBridgeService.pollIncomingMessage()).isNull();
    }

    @Test
    public void testSessionHealthMonitorWorkerUpdatesStatusesCorrectly() {
        // Given accounts with different session integrity issues
        TgAccount accountSpam = new TgAccount();
        accountSpam.setPhoneNumber("+10000000001");
        accountSpam.setStatus("Active");
        accountSpam.setSessionData("This session is spam-blocked temporarily.");
        accountSpam = tgAccountRepository.save(accountSpam);

        TgAccount accountBan = new TgAccount();
        accountBan.setPhoneNumber("+10000000002");
        accountBan.setStatus("Active");
        accountBan.setSessionData("Account has been banned permanently!");
        accountBan = tgAccountRepository.save(accountBan);

        TgAccount accountReauth = new TgAccount();
        accountReauth.setPhoneNumber("+10000000003");
        accountReauth.setStatus("Active");
        accountReauth.setSessionData("Session expired, please reauth");
        accountReauth = tgAccountRepository.save(accountReauth);

        TgAccount accountHealthy = new TgAccount();
        accountHealthy.setPhoneNumber("+10000000004");
        accountHealthy.setStatus("Active");
        accountHealthy.setSessionData("HealthySession123456_ValidToken");
        accountHealthy = tgAccountRepository.save(accountHealthy);

        // When running the session health monitor worker
        healthMonitorWorker.monitorSessionHealth();

        // Then statuses should be updated appropriately
        TgAccount updatedSpam = tgAccountRepository.findById(accountSpam.getId()).orElseThrow();
        assertEquals("Temporary Spam-Block", updatedSpam.getStatus());

        TgAccount updatedBan = tgAccountRepository.findById(accountBan.getId()).orElseThrow();
        assertEquals("Permanent Ban", updatedBan.getStatus());

        TgAccount updatedReauth = tgAccountRepository.findById(accountReauth.getId()).orElseThrow();
        assertEquals("Re-authorization Required", updatedReauth.getStatus());

        TgAccount updatedHealthy = tgAccountRepository.findById(accountHealthy.getId()).orElseThrow();
        assertEquals("Active", updatedHealthy.getStatus());
    }
}
