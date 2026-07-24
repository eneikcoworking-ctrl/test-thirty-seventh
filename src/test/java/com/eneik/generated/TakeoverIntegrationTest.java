package com.eneik.generated;

import com.eneik.generated.controller.ConversationController.*;
import com.eneik.generated.model.*;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TakeoverIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        dialogRepository.deleteAll();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void testManualMessageTransitionsDialogToPaused() {
        // 1. Create a dialogue with status ACTIVE (inbound lead message initializes it)
        Dialog activeDialog = new Dialog("chat_12345", AiState.ACTIVE);
        activeDialog = dialogRepository.save(activeDialog);
        assertEquals(AiState.ACTIVE, activeDialog.getAiState());

        // 2. Fetch the list of conversations and verify it is ACTIVE
        ResponseEntity<ConversationPage> listResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/conversations",
                ConversationPage.class
        );
        assertEquals(200, listResponse.getStatusCode().value());
        assertNotNull(listResponse.getBody());
        assertEquals(1, listResponse.getBody().getContent().size());
        assertEquals("ACTIVE", listResponse.getBody().getContent().get(0).getStatus());

        // 3. Send a manual message from a representative
        String url = "http://localhost:" + port + "/api/v1/conversations/" + activeDialog.getId() + "/messages";
        HttpEntity<String> request = new HttpEntity<>("{\"text\": \"Representative here. How can I help you?\"}", headers);
        ResponseEntity<MessageDto> msgResponse = restTemplate.postForEntity(url, request, MessageDto.class);
        assertEquals(201, msgResponse.getStatusCode().value());

        MessageDto msgDto = msgResponse.getBody();
        assertNotNull(msgDto);
        assertEquals("HUMAN_REPRESENTATIVE", msgDto.getSenderType());
        assertEquals("Representative here. How can I help you?", msgDto.getText());

        // 4. Verify conversation is now PAUSED
        ResponseEntity<ConversationPage> listResponseAfter = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/conversations",
                ConversationPage.class
        );
        assertEquals(200, listResponseAfter.getStatusCode().value());
        assertNotNull(listResponseAfter.getBody());
        assertEquals("PAUSED", listResponseAfter.getBody().getContent().get(0).getStatus());
    }

    @Test
    void testPausedDialogIgnoresLeadReplyWhileActiveDialogAutoReplies() {
        // --- Scenario A: ACTIVE dialogue automatically triggers AI response ---
        Dialog activeDialog = new Dialog("chat_active", AiState.ACTIVE);
        activeDialog = dialogRepository.save(activeDialog);

        // Send a lead message to the active dialogue
        String activeLeadUrl = "http://localhost:" + port + "/api/v1/conversations/" + activeDialog.getId() + "/lead-messages";
        HttpEntity<String> activeLeadRequest = new HttpEntity<>("{\"text\": \"Hello AI\"}", headers);
        ResponseEntity<MessageDto> activeLeadResponse = restTemplate.postForEntity(activeLeadUrl, activeLeadRequest, MessageDto.class);
        assertEquals(201, activeLeadResponse.getStatusCode().value());

        // Verify there are exactly 2 messages in the active dialogue (the lead message + the simulated AI response)
        List<Message> activeMessages = messageRepository.findAll();
        assertEquals(2, activeMessages.size());
        // Check message 1 is USER (LEAD), message 2 is AI
        assertEquals(SenderType.USER, activeMessages.get(0).getSenderType());
        assertEquals(SenderType.AI, activeMessages.get(1).getSenderType());


        // --- Scenario B: PAUSED dialogue ignores the lead reply ---
        // Clear database messages to make assertions simple and isolated
        messageRepository.deleteAll();

        Dialog pausedDialog = new Dialog("chat_paused", AiState.PAUSED);
        pausedDialog = dialogRepository.save(pausedDialog);

        // Send a lead message to the paused dialogue
        String pausedLeadUrl = "http://localhost:" + port + "/api/v1/conversations/" + pausedDialog.getId() + "/lead-messages";
        HttpEntity<String> pausedLeadRequest = new HttpEntity<>("{\"text\": \"Hello human\"}", headers);
        ResponseEntity<MessageDto> pausedLeadResponse = restTemplate.postForEntity(pausedLeadUrl, pausedLeadRequest, MessageDto.class);
        assertEquals(201, pausedLeadResponse.getStatusCode().value());

        // Verify there is exactly 1 message in the paused dialogue (only the lead message; no AI response)
        List<Message> pausedMessages = messageRepository.findAll();
        assertEquals(1, pausedMessages.size());
        assertEquals(SenderType.USER, pausedMessages.get(0).getSenderType());
    }
}
