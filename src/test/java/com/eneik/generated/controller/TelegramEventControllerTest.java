package com.eneik.generated.controller;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TelegramEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TgAccountRepository tgAccountRepository;

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
        tgAccountRepository.deleteAll();
    }

    @Test
    public void testStatusEventUpdatesAccountHealthCorrectly() throws Exception {
        // Given an active account
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+19999999999");
        account.setStatus("Active");
        account = tgAccountRepository.save(account);

        Long accountId = account.getId();

        // 1. When an account is hit with a temporary spam-block
        TelegramEventController.StatusEventRequest spamBlockReq = new TelegramEventController.StatusEventRequest();
        spamBlockReq.setAccountId(accountId);
        spamBlockReq.setStatus("Temporary Spam-Block");

        mockMvc.perform(post("/api/v1/telegram/status-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spamBlockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Temporary Spam-Block")));

        TgAccount updatedAccount = tgAccountRepository.findById(accountId).orElseThrow();
        assertEquals("Temporary Spam-Block", updatedAccount.getStatus());

        // 2. When an account is hit with a permanent ban
        TelegramEventController.StatusEventRequest banReq = new TelegramEventController.StatusEventRequest();
        banReq.setAccountId(accountId);
        banReq.setStatus("Permanent Ban");

        mockMvc.perform(post("/api/v1/telegram/status-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(banReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Permanent Ban")));

        updatedAccount = tgAccountRepository.findById(accountId).orElseThrow();
        assertEquals("Permanent Ban", updatedAccount.getStatus());

        // 3. Confirm transition guard: "Permanent Ban" is terminal, so other status updates (like Temporary Spam-Block) should be ignored
        spamBlockReq.setStatus("Temporary Spam-Block");
        mockMvc.perform(post("/api/v1/telegram/status-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spamBlockReq)))
                .andExpect(status().isOk());

        updatedAccount = tgAccountRepository.findById(accountId).orElseThrow();
        assertEquals("Permanent Ban", updatedAccount.getStatus()); // remains Permanent Ban

        // 4. Given an account successfully re-authorizes, When the re-authorization completes, Then status transitions back to 'Active'
        TelegramEventController.StatusEventRequest reauthReq = new TelegramEventController.StatusEventRequest();
        reauthReq.setAccountId(accountId);
        reauthReq.setStatus("Active");

        mockMvc.perform(post("/api/v1/telegram/status-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reauthReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Active")));

        updatedAccount = tgAccountRepository.findById(accountId).orElseThrow();
        assertEquals("Active", updatedAccount.getStatus());
    }

    @Test
    public void testInboundMessageEventTriggersBackgroundAIEvaluation() throws Exception {
        // Given an active outreach dialog (represented by Conversation in ACTIVE status)
        Long telegramChatId = 123456789L;
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                telegramChatId,
                "Bob Johnson",
                "bob_johnson",
                "+1444444444",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // When a new inbound message is received from a lead
        TelegramEventController.MessageEventRequest req = new TelegramEventController.MessageEventRequest();
        req.setTelegramChatId(telegramChatId);
        req.setLeadName("Bob Johnson");
        req.setText("Tell me more about your pricing");
        req.setLeadUsername("bob_johnson");
        req.setLeadPhone("+1444444444");

        mockMvc.perform(post("/api/v1/telegram/message-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.telegramChatId", is(telegramChatId.intValue())));

        // Since the processing is asynchronous in the background, we poll for up to 1 second
        boolean aiReplied = false;
        for (int i = 0; i < 20; i++) {
            List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(convId, null);
            // We expect 2 messages: 1 from the Lead, and 1 from AI_AGENT (Automated turn)
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

        assertTrue(aiReplied, "AI should have asynchronously evaluated and replied to the inbound lead message in the background");
    }
}
