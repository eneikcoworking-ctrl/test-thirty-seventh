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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    public void testProcessInboundMessageAndGetContext_TruncatesHistory() {
        String chatId = "context_chat_1";

        // Add 5 messages
        for (int i = 1; i <= 5; i++) {
            dialogService.receiveInboundMessage(chatId, "Msg " + i, SenderType.USER);
        }

        // Add 6th message via processInboundMessageAndGetContext, requesting max 3
        List<Message> context = dialogService.processInboundMessageAndGetContext(chatId, "Msg 6", SenderType.USER, 3);

        // Verify we only get 3 messages back in chronological order
        assertThat(context).hasSize(3);
        assertThat(context.get(0).getText()).isEqualTo("Msg 4");
        assertThat(context.get(1).getText()).isEqualTo("Msg 5");
        assertThat(context.get(2).getText()).isEqualTo("Msg 6");
    }

    @Test
    public void testProcessInboundMessageAndGetContext_BlocksAfter16Messages() {
        String chatId = "block_chat_1";

        // Add 15 messages (7.5 turns)
        for (int i = 1; i <= 15; i++) {
            dialogService.receiveInboundMessage(chatId, "Msg " + i, SenderType.USER);
        }

        // Add 16th message (completes 8th turn). Should throw exception and stop AI.
        assertThatThrownBy(() -> dialogService.processInboundMessageAndGetContext(chatId, "Msg 16", SenderType.USER, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum allowed turns");

        // Verify AI state is STOPPED
        Optional<Dialog> dialog = dialogRepository.findByTelegramChatId(chatId);
        assertThat(dialog).isPresent();
        assertThat(dialog.get().getAiState()).isEqualTo(AiState.STOPPED);
    }
}
