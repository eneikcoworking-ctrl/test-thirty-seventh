package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedTelegramClient implements TelegramClient {
    private static final Logger log = LoggerFactory.getLogger(SimulatedTelegramClient.class);

    @Override
    public void sendTypingSignal(String phoneNumber, String leadIdentifier) {
        log.info("Sending typing status signal from account {} to lead {}", phoneNumber, leadIdentifier);
    }

    @Override
    public void sendMessage(String phoneNumber, String leadIdentifier, String message) {
        log.info("Sending message from account {} to lead {}: {}", phoneNumber, leadIdentifier, message);
    }
}
