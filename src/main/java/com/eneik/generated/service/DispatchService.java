package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.TelegramException;
import com.eneik.generated.exception.TelegramFloodWaitException;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramClient telegramClient;

    public DispatchService(TgAccountRepository tgAccountRepository, TelegramClient telegramClient) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramClient = telegramClient;
    }

    /**
     * Attempts to dispatch a message to a given chatId.
     * Finds the next available active account that hasn't hit its limit.
     * Rotates/reassigns on limit hit or FLOOD_WAIT error.
     */
    @Transactional
    public void dispatchMessage(String chatId, String text) throws TelegramException {
        // Find all accounts with status = 'Active' (or 'ACTIVE', we should support both dynamically or perform a case-insensitive match or match status as defined in tests/db).
        // Let's do a programmatic check to find the first suitable account.
        List<TgAccount> accounts = tgAccountRepository.findAll();

        TgAccount selectedAccount = null;
        for (TgAccount account : accounts) {
            if ("Active".equalsIgnoreCase(account.getStatus()) || "ACTIVE".equalsIgnoreCase(account.getStatus())) {
                if (account.getDailySentCount() < account.getDailyLimit()) {
                    selectedAccount = account;
                    break;
                }
            }
        }

        if (selectedAccount == null) {
            throw new TelegramException("No available active Telegram accounts with remaining daily quota.");
        }

        try {
            log.info("Attempting to dispatch message using account: {}", selectedAccount.getPhoneNumber());
            telegramClient.sendMessage(selectedAccount, chatId, text);

            // If success, increment daily sent count
            selectedAccount.setDailySentCount(selectedAccount.getDailySentCount() + 1);
            selectedAccount.setUpdatedAt(LocalDateTime.now());
            tgAccountRepository.save(selectedAccount);
            log.info("Successfully dispatched message and updated count for account: {}", selectedAccount.getPhoneNumber());

        } catch (TelegramFloodWaitException e) {
            log.warn("Telegram FLOOD_WAIT detected for account: {}. Marking as 'Temporary Spam-Block' and rotating...", selectedAccount.getPhoneNumber());
            selectedAccount.setStatus("Temporary Spam-Block");
            selectedAccount.setUpdatedAt(LocalDateTime.now());
            tgAccountRepository.save(selectedAccount);

            // Re-attempt sending using next available account (recursive failover/rotation)
            dispatchMessage(chatId, text);

        } catch (TelegramException e) {
            // Check if limit exceeded error or standard error
            log.error("Telegram error occurred on account: {}", selectedAccount.getPhoneNumber(), e);
            throw e;
        }
    }
}
