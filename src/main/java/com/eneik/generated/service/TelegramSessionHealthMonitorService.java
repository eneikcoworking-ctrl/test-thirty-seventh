package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelegramSessionHealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(TelegramSessionHealthMonitorService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramAccountMonitorService telegramAccountMonitorService;

    public TelegramSessionHealthMonitorService(TgAccountRepository tgAccountRepository,
                                               TelegramAccountMonitorService telegramAccountMonitorService) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramAccountMonitorService = telegramAccountMonitorService;
    }

    /**
     * Exception representing a temporary network failure during session validation.
     */
    public static class TemporaryNetworkException extends Exception {
        public TemporaryNetworkException(String message) {
            super(message);
        }
    }

    /**
     * Periodic background check of all active Telegram account sessions.
     * Triggers based on configurable cron pattern. Default: every 15 minutes.
     */
    @Scheduled(cron = "${telegram.health.monitor.cron:0 0/15 * * * ?}")
    public void runSessionHealthCheck() {
        log.info("Starting periodic Telegram session health check.");
        List<TgAccount> accounts = tgAccountRepository.findAll();

        for (TgAccount account : accounts) {
            String currentStatus = account.getStatus();

            // Skip verification if already marked as "Permanent Ban" (case-insensitive)
            if ("Permanent Ban".equalsIgnoreCase(currentStatus)) {
                log.info("Skipping session integrity check for account ID: {} (Phone: {}) because it is already marked as Permanent Ban.",
                        account.getId(), account.getPhoneNumber());
                continue;
            }

            try {
                String validatedStatus = verifySessionIntegrity(account);
                telegramAccountMonitorService.updateAccountStatus(account.getId(), validatedStatus);
            } catch (TemporaryNetworkException e) {
                log.warn("Temporary network failure encountered during session check for account ID: {} (Phone: {}). Retaining previous status: '{}'.",
                        account.getId(), account.getPhoneNumber(), currentStatus, e);
                // Retain previous status and do not write to the DB. Will retry on next cycle.
            } catch (Exception e) {
                log.error("Failed to execute session health check for account ID: {} (Phone: {}).",
                        account.getId(), account.getPhoneNumber(), e);
            }
        }

        log.info("Finished periodic Telegram session health check.");
    }

    /**
     * Verifies session integrity based on the account's session data.
     * Uses deterministically testable keyword-based matching on sessionData to map specific states.
     */
    public String verifySessionIntegrity(TgAccount account) throws TemporaryNetworkException {
        String sessionData = account.getSessionData();

        if (sessionData == null || sessionData.trim().isEmpty()) {
            log.warn("Session data is null or empty for account ID: {} (Phone: {}). Flagging for re-authorization.",
                    account.getId(), account.getPhoneNumber());
            return "Re-authorization Required";
        }

        String dataLower = sessionData.toLowerCase();

        if (dataLower.contains("network-fail") || dataLower.contains("timeout")) {
            throw new TemporaryNetworkException("Simulated temporary network failure for session verification.");
        }

        if (dataLower.contains("spam-block")) {
            return "Temporary Spam-Block";
        }

        if (dataLower.contains("ban")) {
            return "Permanent Ban";
        }

        if (dataLower.contains("reauth")) {
            return "Re-authorization Required";
        }

        return "Active";
    }
}
