package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TelegramSessionHealthMonitorWorker {

    private static final Logger log = LoggerFactory.getLogger(TelegramSessionHealthMonitorWorker.class);

    private final TgAccountRepository tgAccountRepository;

    public TelegramSessionHealthMonitorWorker(TgAccountRepository tgAccountRepository) {
        this.tgAccountRepository = tgAccountRepository;
    }

    /**
     * Periodically monitors the health and integrity of active Telegram account sessions.
     * Checks sessionData for failure signatures and updates account status accordingly.
     */
    @Scheduled(fixedDelayString = "${telegram.session-health.worker.delay-ms:5000}")
    @Transactional
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
                        // Atomically update state directly in DB to avoid read-then-save race conditions
                        transitionAccountStatus(account.getId(), calculatedStatus);
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
     * Atomically guards the transition of the account status from 'Active' to a calculated failure state.
     */
    @Transactional
    public void transitionAccountStatus(Long accountId, String calculatedStatus) {
        int updatedCount = tgAccountRepository.updateStatusFromActiveAtomically(accountId, calculatedStatus);
        if (updatedCount > 0) {
            log.info("Successfully transitioned account ID {} from Active to '{}' using atomically-guarded query.", accountId, calculatedStatus);
        } else {
            log.warn("Atomically-guarded status transition failed for account ID {} - status was not Active or already updated.", accountId);
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
