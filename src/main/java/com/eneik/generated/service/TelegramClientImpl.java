package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.TelegramException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramClientImpl implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClientImpl.class);

    @Override
    public void sendMessage(TgAccount account, String chatId, String text) throws TelegramException {
        // Placeholder/no-op implementation for Telegram TDLib / API layer integration.
        log.info("Sending message via Telegram API to chatId: {} using account: {}", chatId, account.getPhoneNumber());
    }
}
