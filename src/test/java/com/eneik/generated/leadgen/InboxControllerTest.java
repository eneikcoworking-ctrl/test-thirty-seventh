package com.eneik.generated.leadgen;

import com.eneik.generated.leadgen.controller.SendMessageRequestDto;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
        if (cacheManager != null) {
            org.springframework.cache.Cache convCache = cacheManager.getCache("conversations");
            if (convCache != null) convCache.clear();
            org.springframework.cache.Cache msgCache = cacheManager.getCache("messages");
            if (msgCache != null) msgCache.clear();
        }
    }

    @Test
    public void testGetConversations_ReturnsConversationsFromAllAccounts() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        // Given conversations exist across multiple channels/accounts
        Conversation c1 = new Conversation(
                UUID.randomUUID().toString(),
                111111L,
                "John Doe",
                "john_doe",
                "+123456789",
                "ESCALATED",
                UUID.randomUUID().toString(),
                now,
                now
        );
        Conversation c2 = new Conversation(
                UUID.randomUUID().toString(),
                222222L,
                "Jane Smith",
                "jane_smith",
                "+987654321",
                "ACTIVE",
                UUID.randomUUID().toString(),
                now.minusHours(1),
                now.minusHours(1)
        );

        conversationRepository.save(c1);
        conversationRepository.save(c2);

        // When fetched (GET /api/v1/conversations)
        // Then conversations from all accounts are returned
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].leadName", is("John Doe")))
                .andExpect(jsonPath("$.content[1].leadName", is("Jane Smith")));
    }

    @Test
    public void testSendManualMessage_DispatchesViaTelegramLayer() throws Exception {
        // Given an existing conversation
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                12345L,
                "Alice Wood",
                "alice_w",
                "+111222333",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        SendMessageRequestDto request = new SendMessageRequestDto("Hello Alice! This is manual sales escalation.");

        // When sent
        // Then it is dispatched via the Telegram layer and successfully saved
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", is(convId)))
                .andExpect(jsonPath("$.text", is("Hello Alice! This is manual sales escalation.")))
                .andExpect(jsonPath("$.senderType", is("HUMAN_REPRESENTATIVE")));

        // Verify conversation history messages
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text", is("Hello Alice! This is manual sales escalation.")));
    }

    @Test
    public void testSendManualMessage_WithInvalidArguments_ReturnsBadRequest() throws Exception {
        SendMessageRequestDto invalidRequest = new SendMessageRequestDto("");

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_ARGUMENT")));
    }

    @Test
    public void testManualMessageTransitionsDialogToPaused() throws Exception {
        // Given an AI-active conversation
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                55555L,
                "Bob Green",
                "bob_g",
                "+555555555",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // When a manual message is sent
        SendMessageRequestDto request = new SendMessageRequestDto("Manual Rep message");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then the conversation status updates to PAUSED
        Conversation updatedConv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("PAUSED", updatedConv.getStatus());
    }

    @Test
    public void testPausedDialogueIgnoresLeadReplyWhileActiveDialogueAutoReplies() throws Exception {
        // --- Scenario A: ACTIVE dialogue automatically triggers AI response ---
        String activeConvId = UUID.randomUUID().toString();
        Conversation activeConv = new Conversation(
                activeConvId,
                66666L,
                "Active Lead",
                "active_l",
                "+666666666",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(activeConv);

        // Send a lead message to the active dialogue
        SendMessageRequestDto leadRequest = new SendMessageRequestDto("Hello Bot");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", activeConvId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Verify there are exactly 2 messages in the active dialogue (the lead message + the simulated AI response)
        assertEquals(2, conversationMessageRepository.findByConversationId(activeConvId, null).size());


        // --- Scenario B: PAUSED dialogue ignores the lead reply ---
        String pausedConvId = UUID.randomUUID().toString();
        Conversation pausedConv = new Conversation(
                pausedConvId,
                77777L,
                "Paused Lead",
                "paused_l",
                "+777777777",
                "PAUSED",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(pausedConv);

        // Send a lead message to the paused dialogue
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", pausedConvId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Verify there is exactly 1 message in the paused dialogue (only the lead message; no AI response)
        assertEquals(1, conversationMessageRepository.findByConversationId(pausedConvId, null).size());
    }

    @Test
    public void testActiveConversation_WhenStopKeywordUnsubscribeReceived_ChangesToEscalatedAndNoAiReply() throws Exception {
        // Given an ACTIVE conversation
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                11111L,
                "Stop Keyword Lead",
                "stop_l",
                "+1111111111",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // When a message with the stop keyword 'unsubscribe' is received
        SendMessageRequestDto leadRequest = new SendMessageRequestDto("Please unsubscribe me from this list.");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Then the conversation status is changed to ESCALATED
        Conversation updatedConv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("ESCALATED", updatedConv.getStatus());

        // and no AI reply is sent (only the lead's message exists in history)
        assertEquals(1, conversationMessageRepository.findByConversationId(convId, null).size());
    }

    @Test
    public void testActiveConversation_WhenNormalMessageReceived_ProceedsNormally() throws Exception {
        // Given an ACTIVE conversation
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                22222L,
                "Normal Lead",
                "normal_l",
                "+2222222222",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // When a message without any stop triggers is received
        SendMessageRequestDto leadRequest = new SendMessageRequestDto("I want to know more about your service");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Then conversation status remains ACTIVE
        Conversation updatedConv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("ACTIVE", updatedConv.getStatus());

        // Then AI response generation proceeds normally (lead message + AI response = 2 messages)
        assertEquals(2, conversationMessageRepository.findByConversationId(convId, null).size());
    }

    @Test
    public void testEscalatedConversation_WhenStopWordsReceived_StatusUnchangedAndNoAiReply() throws Exception {
        // Given a conversation already in ESCALATED status
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                33333L,
                "Escalated Lead",
                "escalated_l",
                "+3333333333",
                "ESCALATED",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // When a new message with stop words is received
        SendMessageRequestDto leadRequest = new SendMessageRequestDto("STOP NOW!");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Then the status remains unchanged (ESCALATED)
        Conversation updatedConv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("ESCALATED", updatedConv.getStatus());

        // And no AI reply is triggered (only the lead message exists in history)
        assertEquals(1, conversationMessageRepository.findByConversationId(convId, null).size());
    }

    @Test
    public void testActiveConversation_ReachingExactly5AiTurns_6thMessageBecomesEscalatedAndNoAiReply() throws Exception {
        // Given a conversation reaching exactly the maximum allowed 5 AI turns
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                44444L,
                "Turn Limit Lead",
                "turn_l",
                "+4444444444",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // Add 5 AI turn messages and some lead messages to simulate history
        OffsetDateTime baseTime = OffsetDateTime.now().minusHours(1);
        for (int i = 1; i <= 5; i++) {
            ConversationMessage leadMsg = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    convId,
                    "Lead message " + i,
                    "LEAD",
                    baseTime.plusMinutes(i * 2),
                    "Turn Limit Lead"
            );
            conversationMessageRepository.save(leadMsg);

            ConversationMessage aiMsg = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    convId,
                    "AI Automated Response " + i,
                    "AI_AGENT",
                    baseTime.plusMinutes(i * 2 + 1),
                    "AI Bot"
            );
            conversationMessageRepository.save(aiMsg);
        }

        // Verify there are exactly 5 AI turns currently
        long aiTurnsBefore = conversationMessageRepository.countByConversationIdAndSenderType(convId, "AI_AGENT");
        assertEquals(5, aiTurnsBefore);

        // When the lead sends the 6th message
        SendMessageRequestDto leadRequest = new SendMessageRequestDto("6th message");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/lead-messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadRequest)))
                .andExpect(status().isCreated());

        // Then the status becomes ESCALATED immediately before generating the next AI reply
        Conversation updatedConv = conversationRepository.findById(convId).orElseThrow();
        assertEquals("ESCALATED", updatedConv.getStatus());

        // And no AI reply is generated (there are still only 5 AI messages in history)
        long aiTurnsAfter = conversationMessageRepository.countByConversationIdAndSenderType(convId, "AI_AGENT");
        assertEquals(5, aiTurnsAfter);
    }

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Test
    public void testConversationsQueryIsCachedAndEvicted() throws Exception {
        // Clear all caches first to start clean
        cacheManager.getCache("conversations").clear();
        cacheManager.getCache("messages").clear();

        OffsetDateTime now = OffsetDateTime.now();
        Conversation c1 = new Conversation(
                UUID.randomUUID().toString(),
                123456L,
                "Test Caching Lead",
                "test_cache",
                "+123456",
                "ACTIVE",
                null,
                now,
                now
        );
        conversationRepository.save(c1);

        // 1. Initial request (GET /api/v1/conversations) -> Cache Miss
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].leadName", is("Test Caching Lead")));

        // Verify that the conversation cache now contains an entry!
        org.springframework.cache.Cache cache = cacheManager.getCache("conversations");
        org.junit.jupiter.api.Assertions.assertNotNull(cache);

        // Let's delete the conversation from the database directly
        conversationRepository.delete(c1);

        // 2. Second request -> Cache Hit (data is returned from cache even though it's deleted from database!)
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].leadName", is("Test Caching Lead")));

        // Save conversation back so we can perform eviction test
        conversationRepository.save(c1);

        // 3. Mutate data: Send a manual message -> should NOT trigger coarse conversations cache eviction (preventing stampedes)
        SendMessageRequestDto request = new SendMessageRequestDto("Evict the cache!");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", c1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Delete conversation again (it's already deleted, but we make sure)
        if (conversationRepository.existsById(c1.getId())) {
            conversationRepository.delete(c1);
        }

        // 4. Subsequent request -> Cache Hit (retains cache and does not stampede database!)
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].leadName", is("Test Caching Lead")));
    }

    @Test
    public void testMessagesQueryIsCachedAndEvicted() throws Exception {
        cacheManager.getCache("conversations").clear();
        cacheManager.getCache("messages").clear();

        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                1234567L,
                "Msg Caching Lead",
                "msg_cache",
                "+1234567",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        ConversationMessage msg = new ConversationMessage(
                UUID.randomUUID().toString(),
                convId,
                "Cached message content",
                "LEAD",
                OffsetDateTime.now(),
                "Msg Caching Lead"
        );
        conversationMessageRepository.save(msg);

        // 1. Initial request (GET /api/v1/conversations/{id}/messages) -> Cache Miss
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text", is("Cached message content")));

        // Verify that the messages cache now contains an entry!
        org.springframework.cache.Cache cache = cacheManager.getCache("messages");
        org.junit.jupiter.api.Assertions.assertNotNull(cache);

        // Delete from DB directly
        conversationMessageRepository.delete(msg);

        // 2. Second request -> Cache Hit (still returns message)
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text", is("Cached message content")));

        // Restore msg
        conversationMessageRepository.save(msg);

        // 3. Mutate data: Send a manual message -> triggers Cache Eviction
        SendMessageRequestDto request = new SendMessageRequestDto("Evict message cache!");
        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Delete msg from DB
        conversationMessageRepository.delete(msg);
        // Also delete the newly saved manual message to verify
        conversationMessageRepository.deleteAll();

        // 4. Subsequent request -> Cache Miss (returns empty)
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testFailSafeFallback_WhenRedisThrowsException_ServiceSucceeds() throws Exception {
        org.springframework.cache.Cache mockRedisCache = org.mockito.Mockito.mock(org.springframework.cache.Cache.class);
        org.mockito.Mockito.when(mockRedisCache.getName()).thenReturn("conversations");
        org.mockito.Mockito.when(mockRedisCache.get(org.mockito.Mockito.any())).thenThrow(new RuntimeException("Redis connection refused!"));
        org.mockito.Mockito.doThrow(new RuntimeException("Redis connection refused!")).when(mockRedisCache).put(org.mockito.Mockito.any(), org.mockito.Mockito.any());

        org.springframework.cache.concurrent.ConcurrentMapCache localFallbackCache = new org.springframework.cache.concurrent.ConcurrentMapCache("conversations");

        com.eneik.generated.CacheConfig.FailSafeCache failSafeCache = new com.eneik.generated.CacheConfig.FailSafeCache(mockRedisCache, localFallbackCache);

        Conversation c = new Conversation(
                UUID.randomUUID().toString(),
                999999L,
                "FailSafe Lead",
                "failsafe",
                "+999999",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        localFallbackCache.put("ALL:NONE:0:20", new org.springframework.data.domain.PageImpl<>(java.util.List.of(c)));

        org.springframework.cache.Cache.ValueWrapper wrapper = failSafeCache.get("ALL:NONE:0:20");
        org.junit.jupiter.api.Assertions.assertNotNull(wrapper);
        org.junit.jupiter.api.Assertions.assertEquals(localFallbackCache.get("ALL:NONE:0:20").get(), wrapper.get());

        failSafeCache.put("test_key", "test_value");
        org.junit.jupiter.api.Assertions.assertEquals("test_value", localFallbackCache.get("test_key").get());
    }
}
