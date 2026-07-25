package com.eneik.generated.leadgen.service;

import com.eneik.generated.leadgen.event.InboundMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramInboundBackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(TelegramInboundBackgroundWorker.class);

    private final TelegramBridgeService telegramBridgeService;
    private final ApplicationEventPublisher eventPublisher;

    public TelegramInboundBackgroundWorker(TelegramBridgeService telegramBridgeService,
                                           ApplicationEventPublisher eventPublisher) {
        this.telegramBridgeService = telegramBridgeService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Periodically checks and processes any new incoming messages from Telegram.
     * Keeps resource usage minimal by returning immediately if the queue is empty.
     */
    @Scheduled(fixedDelayString = "${telegram.inbound.worker.delay-ms:1000}")
    public void checkForInboundMessages() {
        log.trace("Background inbound worker checking for new messages...");

        try {
            TelegramBridgeService.IncomingTelegramMessage incomingMessage;
            while ((incomingMessage = telegramBridgeService.pollIncomingMessage()) != null) {
                try {
                    log.info("Worker detected a new inbound message.");
                    validateIncomingMessage(incomingMessage);

                    log.info("Publishing InboundMessageEvent for chat ID: {}", incomingMessage.getTelegramChatId());
                    InboundMessageEvent event = new InboundMessageEvent(
                            incomingMessage.getTelegramChatId(),
                            incomingMessage.getLeadName(),
                            incomingMessage.getText(),
                            incomingMessage.getLeadUsername(),
                            incomingMessage.getLeadPhone()
                    );
                    eventPublisher.publishEvent(event);
                } catch (IllegalArgumentException e) {
                    // Graceful handling of malformed messages: log error and continue without crashing
                    log.error("Malformed inbound message received: " + e.getMessage());
                } catch (Exception e) {
                    // Catch unexpected message processing exceptions to prevent thread interruption
                    log.error("Error processing inbound message: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Unhandled exception in inbound background worker loop: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the inbound message contains all required fields.
     */
    private void validateIncomingMessage(TelegramBridgeService.IncomingTelegramMessage message) {
        if (message.getTelegramChatId() == null) {
            throw new IllegalArgumentException("telegramChatId is required and cannot be null");
        }
        if (message.getLeadName() == null || message.getLeadName().trim().isEmpty()) {
            throw new IllegalArgumentException("leadName is required and cannot be empty");
        }
        if (message.getText() == null || message.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("text is required and cannot be empty");
        }
    }
}
