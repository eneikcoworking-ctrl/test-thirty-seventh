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
}
