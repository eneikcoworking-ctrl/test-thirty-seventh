package com.eneik.generated.controller;

import com.eneik.generated.dto.TelegramAccountStatusRequest;
import com.eneik.generated.dto.TelegramInboundMessageRequest;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.service.TelegramBackgroundEventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telegram/events")
public class TelegramEventListenerController {

    private static final String INVALID_REQUEST_CODE = "INVALID_REQUEST";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private static final Logger log = LoggerFactory.getLogger(TelegramEventListenerController.class);

    private final TelegramBackgroundEventListener telegramBackgroundEventListener;

    public TelegramEventListenerController(TelegramBackgroundEventListener telegramBackgroundEventListener) {
        this.telegramBackgroundEventListener = telegramBackgroundEventListener;
    }

    @PostMapping("/message")
    public ResponseEntity<?> receiveInboundMessage(@RequestBody TelegramInboundMessageRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(INVALID_REQUEST_CODE, "Request body must not be null"));
        }

        try {
            telegramBackgroundEventListener.handleInboundMessage(
                    request.getTelegramChatId(),
                    request.getText(),
                    request.getMediaType()
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (Exception e) {
            log.error("Failed to process inbound message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(INTERNAL_ERROR_CODE, "An internal error occurred while processing the message."));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> receiveAccountStatusUpdate(@RequestBody TelegramAccountStatusRequest request) {
        if (request == null || request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(INVALID_REQUEST_CODE, "Request body and phoneNumber must not be null"));
        }

        try {
            telegramBackgroundEventListener.handleAccountStatusUpdate(
                    request.getPhoneNumber(),
                    request.getStatus()
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (Exception e) {
            log.error("Failed to update account status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(INTERNAL_ERROR_CODE, "An internal error occurred while updating account status."));
        }
    }
}
