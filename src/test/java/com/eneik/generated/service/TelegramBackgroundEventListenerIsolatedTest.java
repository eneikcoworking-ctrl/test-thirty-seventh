package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.InboxService;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramBackgroundEventListenerIsolatedTest {

    @Mock
    private TgAccountRepository tgAccountRepository;
    @Mock
    private DialogRepository dialogRepository;
    @Mock
    private DialogService dialogService;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private InboxService inboxService;

    @InjectMocks
    private TelegramBackgroundEventListener listener;

    @Test
    public void testHandleAccountStatusUpdate_Success() {
        when(tgAccountRepository.updateStatusByPhoneNumber(eq("+123456789"), eq("Banned"), any())).thenReturn(1);
        listener.handleAccountStatusUpdate("+123456789", "Banned");
        verify(tgAccountRepository, times(1)).updateStatusByPhoneNumber(eq("+123456789"), eq("Banned"), any());
    }

    @Test
    public void testHandleInboundMessage_WithCrmConversation() {
        Conversation conv = new Conversation();
        conv.setId("conv-123");
        when(conversationRepository.findByTelegramChatId(100L)).thenReturn(Optional.of(conv));
        when(dialogRepository.findByTelegramChatId("100")).thenReturn(Optional.empty());

        listener.handleInboundMessage(100L, "Hello", "text");

        verify(inboxService, times(1)).receiveLeadMessage("conv-123", "Hello");
        verify(dialogService, never()).receiveInboundMessage(any(), any(), any());
    }

    @Test
    public void testHandleInboundMessage_WithActiveOutboundDialog() {
        when(conversationRepository.findByTelegramChatId(200L)).thenReturn(Optional.empty());

        Dialog dialog = new Dialog();
        dialog.setId(99L);
        dialog.setAiState(AiState.ACTIVE);
        when(dialogRepository.findByTelegramChatId("200")).thenReturn(Optional.of(dialog));

        listener.handleInboundMessage(200L, "Hello AI", "text");

        verify(dialogService, times(1)).receiveInboundMessage("200", "Hello AI", SenderType.USER);
        verify(dialogService, times(1)).generateAiResponse(99L, "Hello AI");
    }
}
