package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TelegramSessionHealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(TelegramSessionHealthMonitorService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramSessionHealthUpdateHelper sessionHealthUpdateHelper;

    public TelegramSessionHealthMonitorService(TgAccountRepository tgAccountRepository,
                                               TelegramSessionHealthUpdateHelper sessionHealthUpdateHelper) {
        this.tgAccountRepository = tgAccountRepository;
        this.sessionHealthUpdateHelper = sessionHealthUpdateHelper;
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
        String runTraceId = UUID.randomUUID().toString();
        log.info("[Run Trace ID: {}] Starting periodic Telegram session health check.", runTraceId);

        List<TgAccount> accounts = tgAccountRepository.findAll();

        for (TgAccount account : accounts) {
            String currentStatus = account.getStatus();
            Long accountId = account.getId();

            // Set logging context with trace ID and account ID
            MDC.put("traceId", runTraceId);
            MDC.put("accountId", String.valueOf(accountId));

            try {
                // Skip verification if already marked as "Permanent Ban" (case-insensitive)
                if ("Permanent Ban".equalsIgnoreCase(currentStatus)) {
                    log.info("[Trace ID: {}, Account ID: {}] Skipping session integrity check because it is already marked as Permanent Ban.",
                            runTraceId, accountId);
                    continue;
                }

                String validatedStatus = verifySessionIntegrity(account);

                if (validatedStatus.equalsIgnoreCase(currentStatus)) {
                    log.info("[Trace ID: {}, Account ID: {}] Account is already in status '{}'. No update required.",
                            runTraceId, accountId, currentStatus);
                    continue;
                }

                // Execute atomically-guarded database update directly using repository
                int updatedCount = sessionHealthUpdateHelper.updateStatusGuarded(
                        accountId, validatedStatus, currentStatus, LocalDateTime.now());

                if (updatedCount == 0) {
                    log.warn("[Trace ID: {}, Account ID: {}] Guarded status update failed (concurrency detected, status changed from '{}' concurrently).",
                            runTraceId, accountId, currentStatus);
                } else {
                    log.info("[Trace ID: {}, Account ID: {}] Successfully updated status from '{}' to '{}'.",
                            runTraceId, accountId, currentStatus, validatedStatus);
                }

            } catch (TemporaryNetworkException e) {
                log.warn("[Trace ID: {}, Account ID: {}] Temporary network failure encountered during session check. Retaining previous status: '{}'.",
                        runTraceId, accountId, currentStatus, e);
                // Retain previous status and do not write to the DB. Will retry on next cycle.
            } catch (Exception e) {
                log.error("[Trace ID: {}, Account ID: {}] Failed to execute session health check.",
                        runTraceId, accountId, e);
            } finally {
                // Clear logging context
                MDC.remove("traceId");
                MDC.remove("accountId");
            }
        }

        log.info("[Run Trace ID: {}] Finished periodic Telegram session health check.", runTraceId);
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

/**
 * Transactional helper for executing atomically-guarded database updates.
 * Exposes a separate bean boundaries to allow proper Spring transaction management.
 */
@Service
class TelegramSessionHealthUpdateHelper {

    private final TgAccountRepository tgAccountRepository;

    public TelegramSessionHealthUpdateHelper(TgAccountRepository tgAccountRepository) {
        this.tgAccountRepository = tgAccountRepository;
    }

    @Transactional
    public int updateStatusGuarded(Long id, String newStatus, String expectedOldStatus, LocalDateTime updatedAt) {
        return tgAccountRepository.updateStatusGuarded(id, newStatus, expectedOldStatus, updatedAt);
    }
}
