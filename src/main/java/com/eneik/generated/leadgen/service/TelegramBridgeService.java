package com.eneik.generated.leadgen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TelegramBridgeService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBridgeService.class);

    private final ConcurrentLinkedQueue<IncomingTelegramMessage> incomingQueue = new ConcurrentLinkedQueue<>();

    /**
     * Emulates message dispatch via the Telegram core layer (JNI/JNA bindings to TDLib).
     */
    public String dispatchMessage(Long telegramChatId, String text) {
        log.info("Dispatching message to Telegram Chat ID: {} - text: '{}'", telegramChatId, text);
        return "tg_msg_" + UUID.randomUUID().toString();
    }

    /**
     * Sends a "typing..." chat status signal prior to dispatching messages.
     * Throws IllegalArgumentException if the chat ID is invalid.
     */
    public void sendTypingStatus(Long telegramChatId) {
        if (telegramChatId == null || telegramChatId <= 0) {
            throw new IllegalArgumentException("Invalid recipient chat ID: " + telegramChatId);
        }
        log.info("Sending typing status to Telegram Chat ID: {}", telegramChatId);
    }

    /**
     * Queues a simulated incoming message for testing or real-time background workers.
     */
    public void queueIncomingMessage(Long telegramChatId, String text, String leadName, String leadUsername, String leadPhone) {
        log.info("Queuing simulated incoming message for chat {}: '{}'", telegramChatId, text);
        incomingQueue.add(new IncomingTelegramMessage(telegramChatId, text, leadName, leadUsername, leadPhone));
    }

    /**
     * Polls the next simulated incoming message from the queue.
     */
    public IncomingTelegramMessage pollIncomingMessage() {
        return incomingQueue.poll();
    }

    /**
     * Clears all simulated incoming messages from the queue. Useful for test cleanup.
     */
    public void clearIncomingMessages() {
        incomingQueue.clear();
    }

    /**
     * Inner class representing a simulated incoming Telegram message.
     */
    public static class IncomingTelegramMessage {
        private final Long telegramChatId;
        private final String text;
        private final String leadName;
        private final String leadUsername;
        private final String leadPhone;

        public IncomingTelegramMessage(Long telegramChatId, String text, String leadName, String leadUsername, String leadPhone) {
            this.telegramChatId = telegramChatId;
            this.text = text;
            this.leadName = leadName;
            this.leadUsername = leadUsername;
            this.leadPhone = leadPhone;
        }

        public Long getTelegramChatId() {
            return telegramChatId;
        }

        public String getText() {
            return text;
        }

        public String getLeadName() {
            return leadName;
        }

        public String getLeadUsername() {
            return leadUsername;
        }

        public String getLeadPhone() {
            return leadPhone;
        }
    }
}
