package com.eneik.generated;

import com.eneik.generated.model.AiEvaluationResult;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.service.AiDialogueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AiDialogueServiceTest {

    @Autowired
    private AiDialogueService aiDialogueService;

    @Autowired
    private DialogRepository dialogRepository;

    @Test
    public void testResponseAdheresToStructuredPersona() {
        String chatId = "chat_adherence_1";
        String systemPrompt = "PREFIX: [AGENT-007]\nSUFFIX: (Confidential)\nTONE: UPPERCASE";
        String userMessage = "hello, ready for dispatch";

        AiEvaluationResult result = aiDialogueService.evaluateAndRespond(chatId, userMessage, systemPrompt);

        assertThat(result).isNotNull();
        assertThat(result.isEscalation()).isFalse();
        assertThat(result.getDetectedIntent()).isNull();
        // The output should be: [AGENT-007] HELLO, READY FOR DISPATCH (Confidential)
        assertThat(result.getResponseText()).isEqualTo("[AGENT-007] HELLO, READY FOR DISPATCH (Confidential)");

        // Verify that the dialog is ACTIVE in DB
        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.ACTIVE);
    }

    @Test
    public void testResponseAdheresToPolitePersona() {
        String chatId = "chat_adherence_2";
        String systemPrompt = "PREFIX: [SUPPORT]\nTONE: POLITE";
        String userMessage = "we need assistance";

        AiEvaluationResult result = aiDialogueService.evaluateAndRespond(chatId, userMessage, systemPrompt);

        assertThat(result).isNotNull();
        assertThat(result.isEscalation()).isFalse();
        assertThat(result.getResponseText()).isEqualTo("[SUPPORT] Dear client, we need assistance");
    }

    @Test
    public void testResponseAdheresToSimpleUnstructuredPersona() {
        String chatId = "chat_adherence_3";
        String systemPrompt = "Friendly helpful AI persona";
        String userMessage = "how are you?";

        AiEvaluationResult result = aiDialogueService.evaluateAndRespond(chatId, userMessage, systemPrompt);

        assertThat(result).isNotNull();
        assertThat(result.isEscalation()).isFalse();
        assertThat(result.getResponseText()).contains("Friendly helpful AI persona");
        assertThat(result.getResponseText()).contains("how are you?");
    }

    @Test
    public void testBookingIntentTriggersEscalationAndStopsAi() {
        String chatId = "chat_booking_escalation";
        String userMessage = "I would like to book a slot for tomorrow";
        String systemPrompt = "PREFIX: [AGENT]\nTONE: UPPERCASE";

        AiEvaluationResult result = aiDialogueService.evaluateAndRespond(chatId, userMessage, systemPrompt);

        assertThat(result).isNotNull();
        assertThat(result.isEscalation()).isTrue();
        assertThat(result.getDetectedIntent()).isEqualTo("BOOKING_REQUEST");
        assertThat(result.getResponseText()).contains("Escalating to a human agent");

        // Verify that the dialog's AI state is STOPPED in DB
        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.STOPPED);
    }

    @Test
    public void testHumanEscalationIntentTriggersEscalationAndStopsAi() {
        String chatId = "chat_human_escalation";
        String userMessage = "Let me speak to a human agent please";
        String systemPrompt = "PREFIX: [AGENT]\nTONE: UPPERCASE";

        AiEvaluationResult result = aiDialogueService.evaluateAndRespond(chatId, userMessage, systemPrompt);

        assertThat(result).isNotNull();
        assertThat(result.isEscalation()).isTrue();
        assertThat(result.getDetectedIntent()).isEqualTo("HUMAN_ESCALATION");
        assertThat(result.getResponseText()).contains("Escalating to a human agent");

        // Verify that the dialog's AI state is STOPPED in DB
        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.STOPPED);
    }
}
