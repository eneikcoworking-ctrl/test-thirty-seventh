package com.eneik.generated.integration;

public interface TelegramClient {
    void sendTypingSignal(String phoneNumber, String leadIdentifier);
    void sendMessage(String phoneNumber, String leadIdentifier, String message);
}
