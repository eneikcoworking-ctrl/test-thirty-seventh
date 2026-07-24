package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.TelegramException;

public interface TelegramClient {
    /**
     * Sends a message to a given chatId using the specified TgAccount.
     * Throws TelegramException or more specific subclasses like TelegramFloodWaitException on failure.
     */
    void sendMessage(TgAccount account, String chatId, String text) throws TelegramException;
}
