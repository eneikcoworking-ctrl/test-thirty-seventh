package com.eneik.generated.controller;

import com.eneik.generated.leadgen.event.InboundMessageEvent;
import com.eneik.generated.service.TelegramAccountMonitorService;
import com.eneik.generated.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramEventController {

    private static final Logger log = LoggerFactory.getLogger(TelegramEventController.class);

    private final TelegramAccountMonitorService telegramAccountMonitorService;
    private final ApplicationEventPublisher eventPublisher;

    public TelegramEventController(TelegramAccountMonitorService telegramAccountMonitorService,
                                   ApplicationEventPublisher eventPublisher) {
        this.telegramAccountMonitorService = telegramAccountMonitorService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/status-event")
    public ResponseEntity<?> receiveStatusEvent(@RequestBody StatusEventRequest request) {
        log.info("Received Telegram status event for account ID: {} -> status: {}", request.getAccountId(), request.getStatus());

        if (request.getAccountId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "accountId is required"
            ));
        }

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "status is required"
            ));
        }

        // Validate allowed statuses: Active, Temporary Spam-Block, Permanent Ban, Re-authorization Required
        String status = request.getStatus().trim();
        boolean isValidStatus = status.equalsIgnoreCase("Active")
                || status.equalsIgnoreCase("Temporary Spam-Block")
                || status.equalsIgnoreCase("Permanent Ban")
                || status.equalsIgnoreCase("Re-authorization Required");

        if (!isValidStatus) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "Invalid status. Must be one of: Active, Temporary Spam-Block, Permanent Ban, Re-authorization Required"
            ));
        }

        try {
            telegramAccountMonitorService.updateAccountStatus(request.getAccountId(), status);
            return ResponseEntity.ok(Map.of(
                    "message", "Account status updated successfully",
                    "accountId", request.getAccountId(),
                    "status", status
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                    "NOT_FOUND",
                    e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Internal server error handling status event for account: " + request.getAccountId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                    "INTERNAL_ERROR",
                    e.getMessage()
            ));
        }
    }

    @PostMapping("/message-event")
    public ResponseEntity<?> receiveMessageEvent(@RequestBody MessageEventRequest request) {
        log.info("Received Telegram message event from chat: {}", request.getTelegramChatId());

        if (request.getTelegramChatId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "telegramChatId is required"
            ));
        }

        if (request.getLeadName() == null || request.getLeadName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "leadName is required"
            ));
        }

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "INVALID_ARGUMENT",
                    "text is required"
            ));
        }

        try {
            // Publish the inbound message event to trigger background worker/AI evaluation asynchronously
            InboundMessageEvent event = new InboundMessageEvent(
                    request.getTelegramChatId(),
                    request.getLeadName(),
                    request.getText(),
                    request.getLeadUsername(),
                    request.getLeadPhone()
            );
            eventPublisher.publishEvent(event);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "message", "Inbound message event accepted and listener triggered in the background",
                    "telegramChatId", request.getTelegramChatId()
            ));
        } catch (Exception e) {
            log.error("Internal server error publishing message event: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                    "INTERNAL_ERROR",
                    e.getMessage()
            ));
        }
    }

    // Static inner request classes for clean payload deserialization

    public static class StatusEventRequest {
        private Long accountId;
        private String status;

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class MessageEventRequest {
        private Long telegramChatId;
        private String leadName;
        private String text;
        private String leadUsername;
        private String leadPhone;

        public Long getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(Long telegramChatId) {
            this.telegramChatId = telegramChatId;
        }

        public String getLeadName() {
            return leadName;
        }

        public void setLeadName(String leadName) {
            this.leadName = leadName;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getLeadUsername() {
            return leadUsername;
        }

        public void setLeadUsername(String leadUsername) {
            this.leadUsername = leadUsername;
        }

        public String getLeadPhone() {
            return leadPhone;
        }

        public void setLeadPhone(String leadPhone) {
            this.leadPhone = leadPhone;
        }
    }
}
