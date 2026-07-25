package com.eneik.generated.controller;

import com.eneik.generated.dto.TelegramAccountStatusRequest;
import com.eneik.generated.dto.TelegramInboundMessageRequest;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.service.TelegramBackgroundEventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/telegram/events")
public class TelegramEventListenerController {

    private static final Logger log = LoggerFactory.getLogger(TelegramEventListenerController.class);
    private final TelegramBackgroundEventListener telegramBackgroundEventListener;

    public TelegramEventListenerController(TelegramBackgroundEventListener telegramBackgroundEventListener) {
        this.telegramBackgroundEventListener = telegramBackgroundEventListener;
    }

    @PostMapping("/message")
    public ResponseEntity<?> receiveInboundMessage(@RequestBody TelegramInboundMessageRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_REQUEST", "Request body must not be null"));
        }

        try {
            telegramBackgroundEventListener.handleInboundMessage(
                    request.getTelegramChatId(),
                    request.getText(),
                    request.getMediaType()
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (Exception e) {
            log.error("Internal error processing inbound message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An internal server error occurred while processing the event."));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> receiveAccountStatusUpdate(@RequestBody TelegramAccountStatusRequest request) {
        if (request == null || request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_REQUEST", "Request body and phoneNumber must not be null"));
        }

        try {
            telegramBackgroundEventListener.handleAccountStatusUpdate(
                    request.getPhoneNumber(),
                    request.getStatus()
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (Exception e) {
            log.error("Internal error processing account status update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An internal server error occurred while processing the event."));
        }
    }
}
