package com.eneik.generated.controller;

import com.eneik.generated.dto.TelegramAccountStatusRequest;
import com.eneik.generated.dto.TelegramInboundMessageRequest;
import com.eneik.generated.service.TelegramBackgroundEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramEventListenerControllerTest {

    @Mock
    private TelegramBackgroundEventListener listener;

    @InjectMocks
    private TelegramEventListenerController controller;

    @Test
    public void testReceiveInboundMessage_NullRequest() {
        ResponseEntity<?> response = controller.receiveInboundMessage(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testReceiveInboundMessage_Success() {
        TelegramInboundMessageRequest req = new TelegramInboundMessageRequest();
        req.setTelegramChatId(123L);
        req.setText("hello");
        req.setMediaType("text");

        doNothing().when(listener).handleInboundMessage(123L, "hello", "text");

        ResponseEntity<?> response = controller.receiveInboundMessage(req);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void testReceiveInboundMessage_InternalError() {
        TelegramInboundMessageRequest req = new TelegramInboundMessageRequest();
        req.setTelegramChatId(123L);

        doThrow(new RuntimeException("DB down")).when(listener).handleInboundMessage(any(), any(), any());

        ResponseEntity<?> response = controller.receiveInboundMessage(req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        // Verify internal error msg doesn't expose "DB down" string
        // We'll just verify status here, full json mapping tests would be better but this covers the core logic
    }

    @Test
    public void testReceiveAccountStatusUpdate_NullRequest() {
        ResponseEntity<?> response = controller.receiveAccountStatusUpdate(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testReceiveAccountStatusUpdate_NullPhone() {
        TelegramAccountStatusRequest req = new TelegramAccountStatusRequest();
        ResponseEntity<?> response = controller.receiveAccountStatusUpdate(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
