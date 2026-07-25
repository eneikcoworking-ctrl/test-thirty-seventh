package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class TelegramBackgroundEventListenerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelegramBackgroundEventListener eventListener;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        conversationMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        messageRepository.deleteAll();
        dialogRepository.deleteAll();
        tgAccountRepository.deleteAll();
    }

    @Test
    public void testHandleAccountStatusUpdate_Success() {
        // Given an existing connected Telegram account
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380991111111");
        account.setStatus("Active");
        tgAccountRepository.saveAndFlush(account);

        // When a background account status update is processed (e.g. gets temporary spam-block)
        eventListener.handleAccountStatusUpdate("+380991111111", "Temporary Spam-Block");

        // Then its status is immediately updated in the database
        TgAccount updatedAccount = tgAccountRepository.findByPhoneNumber("+380991111111").orElseThrow();
        assertThat(updatedAccount.getStatus()).isEqualTo("Temporary Spam-Block");

        // When updated to permanent ban
        eventListener.handleAccountStatusUpdate("+380991111111", "Permanent Ban");

        // Then it updates correctly
        updatedAccount = tgAccountRepository.findByPhoneNumber("+380991111111").orElseThrow();
        assertThat(updatedAccount.getStatus()).isEqualTo("Permanent Ban");
    }

    @Test
    public void testHandleInboundMessage_RoutesToCRMConversationAndTriggersAI() {
        // Given an active CRM Conversation
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                888222L,
                "CRM Lead",
                "crm_lead",
                "+12345678",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.saveAndFlush(conv);

        // When an inbound message event is processed by the background listener
        eventListener.handleInboundMessage(888222L, "Hi! I want to try your platform.", "text");

        // Then it is stored and triggers AI turn (Lead msg + AI Automated Response)
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(convId, null);
        assertThat(messages).hasSize(2);

        ConversationMessage leadMsg = messages.stream().filter(m -> "LEAD".equals(m.getSenderType())).findFirst().orElseThrow();
        assertThat(leadMsg.getText()).isEqualTo("Hi! I want to try your platform.");

        ConversationMessage aiMsg = messages.stream().filter(m -> "AI_AGENT".equals(m.getSenderType())).findFirst().orElseThrow();
        assertThat(aiMsg.getText()).contains("AI Automated Response to: Hi! I want to try your platform.");
    }

    @Test
    public void testHandleInboundMessage_RoutesToOutboundDialogAndTriggersAI() {
        // Given an active outbound Dialog
        Dialog dialog = new Dialog("777333", AiState.ACTIVE);
        dialogRepository.saveAndFlush(dialog);

        // When an inbound message event is processed
        eventListener.handleInboundMessage(777333L, "Yes, please tell me more.", "text");

        // Then Dialog's message list should contain USER response and subsequent AI automated turn message
        Optional<Dialog> updatedDialogOpt = dialogRepository.findByTelegramChatId("777333");
        assertThat(updatedDialogOpt).isPresent();
        Dialog updatedDialog = updatedDialogOpt.get();

        long messageCount = messageRepository.countByDialogId(updatedDialog.getId());
        assertThat(messageCount).isEqualTo(2);

        List<Message> messages = messageRepository.findAll();
        Message userMessage = messages.stream().filter(m -> m.getSenderType() == SenderType.USER).findFirst().orElseThrow();
        assertThat(userMessage.getText()).isEqualTo("Yes, please tell me more.");

        Message aiMessage = messages.stream().filter(m -> m.getSenderType() == SenderType.AI).findFirst().orElseThrow();
        assertThat(aiMessage.getText()).isEqualTo("AI Automated Response to: Yes, please tell me more.");
    }

    @Test
    public void testHandleInboundMessage_EmptyPayloadOrUnsupportedMedia_HandledGracefully() {
        // Given an active CRM Conversation and active Dialog
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                999999L,
                "Test Lead",
                "test_lead",
                "+12345",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.saveAndFlush(conv);

        Dialog dialog = new Dialog("999999", AiState.ACTIVE);
        dialogRepository.saveAndFlush(dialog);

        // Scenario A: Empty/null text payload
        eventListener.handleInboundMessage(999999L, "", "text");
        eventListener.handleInboundMessage(999999L, null, "text");

        // Scenario B: Unsupported media type
        eventListener.handleInboundMessage(999999L, "Some photo caption", "photo");
        eventListener.handleInboundMessage(999999L, "Document attachment", "document");

        // Verify that no messages were saved in either repository (failure was handled gracefully without crash)
        assertThat(conversationMessageRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    public void testController_EndpointsWorkAsExpected() throws Exception {
        // Setup initial DB states
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380992222222");
        account.setStatus("Active");
        tgAccountRepository.saveAndFlush(account);

        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                555666L,
                "Web Lead",
                "web_lead",
                "+12345678",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.saveAndFlush(conv);

        // 1. Test POST /api/v1/telegram/events/status
        Map<String, String> statusPayload = new HashMap<>();
        statusPayload.put("phoneNumber", "+380992222222");
        statusPayload.put("status", "Re-authorization Required");

        mockMvc.perform(post("/api/v1/telegram/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusPayload)))
                .andExpect(status().isAccepted());

        TgAccount updatedAccount = tgAccountRepository.findByPhoneNumber("+380992222222").orElseThrow();
        assertThat(updatedAccount.getStatus()).isEqualTo("Re-authorization Required");

        // 2. Test POST /api/v1/telegram/events/message
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("telegramChatId", 555666L);
        messagePayload.put("text", "I am interested");
        messagePayload.put("mediaType", "text");

        mockMvc.perform(post("/api/v1/telegram/events/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messagePayload)))
                .andExpect(status().isAccepted());

        List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(convId, null);
        assertThat(messages).hasSize(2);
    }
}
