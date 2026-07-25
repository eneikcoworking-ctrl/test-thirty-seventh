package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TelegramAccountMonitorService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAccountMonitorService.class);

    private final TgAccountRepository tgAccountRepository;

    public TelegramAccountMonitorService(TgAccountRepository tgAccountRepository) {
        this.tgAccountRepository = tgAccountRepository;
    }

    /**
     * Updates the status of a Telegram account dynamically, reflecting its health in real-time.
     * Implements atomically-guarded updates to protect against concurrent modification race conditions.
     */
    @Transactional
    public void updateAccountStatus(Long accountId, String newStatus) {
        log.info("Request to update Telegram account ID {} status to: {}", accountId, newStatus);

        TgAccount account = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram account not found with ID: " + accountId));

        String oldStatus = account.getStatus();

        // Guard rules: "Permanent Ban" is terminal, so do not allow non-Active transitions if already in Permanent Ban.
        // But if we are re-authorizing (newStatus is "Active"), then we allow the transition to "Active".
        if ("Permanent Ban".equalsIgnoreCase(oldStatus) && !"Active".equalsIgnoreCase(newStatus)) {
            log.warn("Telegram account ID {} is in terminal status 'Permanent Ban'. Ignoring transition to '{}'.", accountId, newStatus);
            return;
        }

        if (newStatus.equalsIgnoreCase(oldStatus)) {
            log.info("Telegram account ID {} is already in status '{}'. No update required.", accountId, newStatus);
            return;
        }

        // Atomically-guarded DB update to prevent concurrent request race conditions
        int updatedCount = tgAccountRepository.updateStatusGuarded(accountId, newStatus, oldStatus, LocalDateTime.now());
        if (updatedCount == 0) {
            log.warn("Guarded status update failed for account ID {} (concurrency detected). Retrying...", accountId);
            // Re-fetch and apply atomically
            TgAccount reloaded = tgAccountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Telegram account not found with ID: " + accountId));

            String currentStatus = reloaded.getStatus();
            if ("Permanent Ban".equalsIgnoreCase(currentStatus) && !"Active".equalsIgnoreCase(newStatus)) {
                log.warn("Account {} became 'Permanent Ban' concurrently. Skipping transition.", accountId);
                return;
            }

            reloaded.setStatus(newStatus);
            reloaded.setUpdatedAt(LocalDateTime.now());
            tgAccountRepository.save(reloaded);
        }

        log.info("Successfully updated Telegram account ID {} status from '{}' to '{}'.", accountId, oldStatus, newStatus);
    }
}
