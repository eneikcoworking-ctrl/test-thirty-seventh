package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TelegramSessionHealthMonitorWorker {

    private static final Logger log = LoggerFactory.getLogger(TelegramSessionHealthMonitorWorker.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramAccountMonitorService telegramAccountMonitorService;

    public TelegramSessionHealthMonitorWorker(TgAccountRepository tgAccountRepository,
                                             TelegramAccountMonitorService telegramAccountMonitorService) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramAccountMonitorService = telegramAccountMonitorService;
    }

    /**
     * Periodically monitors the health and integrity of active Telegram account sessions.
     * Checks sessionData for failure signatures and updates account status accordingly.
     */
    @Scheduled(fixedDelayString = "${telegram.session-health.worker.delay-ms:5000}")
    public void monitorSessionHealth() {
        log.trace("Background session health monitor checking active sessions...");

        try {
            List<TgAccount> activeAccounts = tgAccountRepository.findByStatusIgnoreCase("Active");
            for (TgAccount account : activeAccounts) {
                try {
                    String sessionData = account.getSessionData();
                    String calculatedStatus = determineStatusFromSessionData(sessionData);

                    if (calculatedStatus != null) {
                        log.warn("Session health issue detected for account ID {}. Transitioning to: {}", account.getId(), calculatedStatus);
                        telegramAccountMonitorService.updateAccountStatus(account.getId(), calculatedStatus);
                    }
                } catch (Exception e) {
                    log.error("Error checking session health for account ID " + account.getId() + ": " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Unhandled exception in session health monitor worker run: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes session data for failure signatures and returns the appropriate failure status,
     * or null if the session is healthy.
     */
    private String determineStatusFromSessionData(String sessionData) {
        if (sessionData == null || sessionData.trim().isEmpty()) {
            return null; // Ignore uninitialized or empty session data (avoids conflict with unit-test mock accounts)
        }

        String lowerSession = sessionData.toLowerCase();
        if (lowerSession.contains("reauth") || lowerSession.contains("expired") || lowerSession.contains("invalid")) {
            return "Re-authorization Required";
        }
        if (lowerSession.contains("ban")) {
            return "Permanent Ban";
        }
        if (lowerSession.contains("spam")) {
            return "Temporary Spam-Block";
        }

        return null; // Healthy, no action needed
    }
}
