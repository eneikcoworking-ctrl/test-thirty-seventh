package com.eneik.generated.leadgen;

import com.eneik.generated.leadgen.controller.ManualMessageRequestDto;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.Lead;
import com.eneik.generated.leadgen.model.TelegramAccount;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.repository.LeadRepository;
import com.eneik.generated.leadgen.repository.TelegramAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

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
    private TelegramAccountRepository telegramAccountRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        conversationRepository.deleteAll();
        leadRepository.deleteAll();
        telegramAccountRepository.deleteAll();
    }

    @Test
    public void testGetInboxChats_ReturnsConversationsFromAllAccounts() throws Exception {
        // Given conversations exist across multiple accounts
        TelegramAccount acc1 = telegramAccountRepository.save(new TelegramAccount("acc_1", "+123456789", "Active"));
        TelegramAccount acc2 = telegramAccountRepository.save(new TelegramAccount("acc_2", "+987654321", "Active"));

        Lead lead1 = leadRepository.save(new Lead("lead_a", "john_doe", "+111222333"));
        Lead lead2 = leadRepository.save(new Lead("lead_b", "jane_smith", "+444555666"));

        conversationRepository.save(new Conversation("acc_1", "lead_a", "john_doe", "Hello John!", OffsetDateTime.now()));
        conversationRepository.save(new Conversation("acc_2", "lead_b", "jane_smith", "Hi Jane!", OffsetDateTime.now()));

        // When fetched
        // Then conversations from all accounts are returned
        mockMvc.perform(get("/api/inbox/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].telegramAccountId", is("acc_1")))
                .andExpect(jsonPath("$[0].leadUsername", is("john_doe")))
                .andExpect(jsonPath("$[0].lastMessage", is("Hello John!")))
                .andExpect(jsonPath("$[1].telegramAccountId", is("acc_2")))
                .andExpect(jsonPath("$[1].leadUsername", is("jane_smith")))
                .andExpect(jsonPath("$[1].lastMessage", is("Hi Jane!")));
    }

    @Test
    public void testSendManualMessage_DispatchesViaTelegramLayer() throws Exception {
        // Given a manual message request
        ManualMessageRequestDto request = new ManualMessageRequestDto("acc_1", "lead_a", "This is a manual sales escalation message");

        // When sent
        // Then it is dispatched via the Telegram layer
        mockMvc.perform(post("/api/inbox/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramAccountId", is("acc_1")))
                .andExpect(jsonPath("$.leadId", is("lead_a")))
                .andExpect(jsonPath("$.message", is("This is a manual sales escalation message")))
                .andExpect(jsonPath("$.status", is("SENT")));

        // Verify a conversation was updated or created
        mockMvc.perform(get("/api/inbox/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].telegramAccountId", is("acc_1")))
                .andExpect(jsonPath("$[0].leadId", is("lead_a")))
                .andExpect(jsonPath("$[0].lastMessage", is("This is a manual sales escalation message")));
    }

    @Test
    public void testSendManualMessage_WithInvalidArguments_ReturnsBadRequest() throws Exception {
        ManualMessageRequestDto invalidRequest = new ManualMessageRequestDto("", "lead_a", "Hello");

        mockMvc.perform(post("/api/inbox/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_ARGUMENT")))
                .andExpect(jsonPath("$.errorMessage", is("telegramAccountId is required")));
    }
}
