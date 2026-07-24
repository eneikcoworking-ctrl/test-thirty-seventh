package com.eneik.generated.leadgen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TelegramBridgeService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBridgeService.class);

    /**
     * Emulates message dispatch via the Telegram core layer (JNI/JNA bindings to TDLib).
     */
    public String dispatchMessage(Long telegramChatId, String text) {
        log.info("Dispatching message to Telegram Chat ID: {} - text: '{}'", telegramChatId, text);
        return "tg_msg_" + UUID.randomUUID().toString();
    }
}
