package com.eneik.generated.controller;

import com.eneik.generated.dto.TelegramAccountStatusRequest;
import com.eneik.generated.dto.TelegramInboundMessageRequest;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.service.TelegramBackgroundEventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telegram/events")
public class TelegramEventListenerController {

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_SERVER_ERROR", e.getMessage()));
        }
    }
}
