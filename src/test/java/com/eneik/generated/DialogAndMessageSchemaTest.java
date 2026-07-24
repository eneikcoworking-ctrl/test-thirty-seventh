package com.eneik.generated;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.service.DialogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DialogAndMessageSchemaTest {

    @Autowired
    private DialogService dialogService;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void testInboundMessageStoredWithSenderTypeAndDialog() {
        // Given an inbound message
        String chatId = "test_chat_123";
        String messageText = "Hello from lead!";
        SenderType senderType = SenderType.USER;

        // When received
        Message savedMessage = dialogService.receiveInboundMessage(chatId, messageText, senderType);

        // Then it is stored with sender type in the Messages table
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getText()).isEqualTo(messageText);
        assertThat(savedMessage.getSenderType()).isEqualTo(senderType);

        // Verify it links to the Dialog correctly
        Dialog dialog = savedMessage.getDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.getTelegramChatId()).isEqualTo(chatId);
        assertThat(dialog.getAiState()).isEqualTo(AiState.ACTIVE);

        // Verify dialog is saved in database
        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getId()).isEqualTo(dialog.getId());
    }

    @Test
    public void testStopTriggerUpdatesAiState() {
        // Given a dialog
        String chatId = "test_chat_456";
        dialogService.receiveInboundMessage(chatId, "Hello", SenderType.USER);

        // When a stop-trigger fires
        Dialog updatedDialog = dialogService.handleStopTrigger(chatId, AiState.STOPPED);

        // Then its AI state is updated in the DB
        assertThat(updatedDialog).isNotNull();
        assertThat(updatedDialog.getAiState()).isEqualTo(AiState.STOPPED);

        Optional<Dialog> dbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dbDialog).isPresent();
        assertThat(dbDialog.get().getAiState()).isEqualTo(AiState.STOPPED);
    }

    @Test
    public void testStopTriggerInboundMessageTransitionsToAwaitingHumanIntervention() {
        // Given a dialog
        String chatId = "test_chat_stop";
        dialogService.receiveInboundMessage(chatId, "Hello", SenderType.USER);

        // Verify initial state is ACTIVE
        Optional<Dialog> initialDbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(initialDbDialog).isPresent();
        assertThat(initialDbDialog.get().getAiState()).isEqualTo(AiState.ACTIVE);

        // When a stop-trigger inbound message from user is processed
        Message savedMessage = dialogService.receiveInboundMessage(chatId, "Please stop messaging me and get me a human operator!", SenderType.USER);

        // Then the dialogue status becomes 'Awaiting Human Intervention'
        Optional<Dialog> updatedDbDialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(updatedDbDialog).isPresent();
        assertThat(updatedDbDialog.get().getAiState()).isEqualTo(AiState.AWAITING_HUMAN_INTERVENTION);
        assertThat(updatedDbDialog.get().getAiState().getDisplayName()).isEqualTo("Awaiting Human Intervention");
    }

    @Test
    public void testAutomatedReplyHaltedOnHaltedDialogue() {
        // Given a dialogue transitioned to AWAITING_HUMAN_INTERVENTION via stop-trigger
        String chatId = "test_chat_halted";
        dialogService.receiveInboundMessage(chatId, "stop", SenderType.USER);

        // Verify automated reply is not allowed
        boolean allowed = dialogService.isAutomatedReplyAllowed(chatId);
        assertThat(allowed).isFalse();

        // When we attempt to generate an automated reply
        Message reply = dialogService.generateAutomatedReply(chatId, "This is an automated reply draft.");

        // Then the AI does not automatically reply (method returns null, no reply created)
        assertThat(reply).isNull();
    }

    @Test
    public void testIncomingMessagesOnHaltedDialogueDoNotAutoReply() {
        // Given a halted dialogue (status is STOPPED)
        String chatId = "test_chat_stopped_direct";
        dialogService.receiveInboundMessage(chatId, "Hello", SenderType.USER);
        dialogService.handleStopTrigger(chatId, AiState.STOPPED);

        // When new messages arrive
        dialogService.receiveInboundMessage(chatId, "Another question?", SenderType.USER);

        // Then automated reply remains disallowed
        boolean allowed = dialogService.isAutomatedReplyAllowed(chatId);
        assertThat(allowed).isFalse();

        // And attempt to reply fails
        Message reply = dialogService.generateAutomatedReply(chatId, "Automated response");
        assertThat(reply).isNull();
    }
}
