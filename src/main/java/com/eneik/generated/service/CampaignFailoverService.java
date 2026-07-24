package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignFailoverService {

    private static final Logger log = LoggerFactory.getLogger(CampaignFailoverService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramBridgeService telegramBridgeService;

    public CampaignFailoverService(TgAccountRepository tgAccountRepository, TelegramBridgeService telegramBridgeService) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramBridgeService = telegramBridgeService;
    }

    /**
     * Dispatches a message for a campaign, with automatic session rotation and failover.
     * Note: This method is NOT annotated with @Transactional to avoid holding database connection
     * open during external network/bridge calls and to ensure intermediate status updates
     * (like marking an account as FLOOD_WAIT) are committed immediately.
     *
     * @param campaignId     the ID of the campaign
     * @param telegramChatId the recipient's Telegram chat ID
     * @param text           the message text to send
     * @return the result detailing success and which account sent the message
     */
    public DispatchResult dispatchWithFailover(String campaignId, Long telegramChatId, String text) {
        while (true) {
            // Find eligible accounts using database-level filtering
            List<TgAccount> eligiblePool = getEligibleAccounts(campaignId);

            if (eligiblePool.isEmpty()) {
                log.error("No eligible Telegram accounts available in pool for campaign: {}", campaignId);
                throw new IllegalStateException("No eligible Telegram accounts available in pool for campaign: " + campaignId);
            }

            TgAccount activeAccount = eligiblePool.get(0);
            log.info("Selected active account ID: {} for dispatch", activeAccount.getId());

            try {
                // Attempt to dispatch via the Telegram bridge (external simulated I/O)
                String messageId = telegramBridgeService.dispatchMessage(telegramChatId, text);

                // If successful, increment the dispatch count in an isolated transaction
                incrementDispatchCount(activeAccount.getId());

                log.info("Successfully dispatched message using account ID: {}.", activeAccount.getId());

                return new DispatchResult(true, activeAccount.getId(), messageId, null);

            } catch (Exception e) {
                // If we encounter a FLOOD_WAIT error, we rotate the task to the next eligible account
                if (isFloodWaitError(e)) {
                    log.warn("Account ID: {} encountered FLOOD_WAIT. Rotating to next eligible account.", activeAccount.getId());
                    updateAccountStatus(activeAccount.getId(), "FLOOD_WAIT");
                    // Continue loop to try the next eligible account
                } else {
                    // For other unexpected exceptions, fail
                    log.error("Failed dispatch on account ID: {} due to unexpected error", activeAccount.getId(), e);
                    return new DispatchResult(false, activeAccount.getId(), null, e.getMessage());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TgAccount> getEligibleAccounts(String campaignId) {
        return tgAccountRepository.findEligibleAccounts(campaignId, "FLOOD_WAIT");
    }

    @Transactional
    public void updateAccountStatus(Long accountId, String status) {
        tgAccountRepository.findById(accountId).ifPresent(account -> {
            account.setStatus(status);
            account.setUpdatedAt(LocalDateTime.now());
            tgAccountRepository.save(account);
        });
    }

    @Transactional
    public void incrementDispatchCount(Long accountId) {
        tgAccountRepository.findById(accountId).ifPresent(account -> {
            account.setDailyDispatchCount(account.getDailyDispatchCount() + 1);
            account.setUpdatedAt(LocalDateTime.now());
            tgAccountRepository.save(account);
        });
    }

    private boolean isFloodWaitError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null && (msg.contains("FLOOD_WAIT") || msg.contains("flood_wait"))) {
            return true;
        }
        if (t.getClass().getSimpleName().contains("FloodWait")) {
            return true;
        }
        return isFloodWaitError(t.getCause());
    }

    /**
     * Resets the daily dispatch counts for all accounts.
     * This is useful for daily scheduler cron tasks.
     */
    @Transactional
    public void resetDailyLimits() {
        log.info("Resetting daily dispatch counts for all Telegram accounts");
        List<TgAccount> accounts = tgAccountRepository.findAll();
        for (TgAccount acc : accounts) {
            acc.setDailyDispatchCount(0);
            if ("FLOOD_WAIT".equalsIgnoreCase(acc.getStatus())) {
                acc.setStatus("Active");
            }
            tgAccountRepository.save(acc);
        }
    }

    public static class DispatchResult {
        private boolean success;
        private Long dispatchedByAccountId;
        private String messageId;
        private String error;

        public DispatchResult() {}

        public DispatchResult(boolean success, Long dispatchedByAccountId, String messageId, String error) {
            this.success = success;
            this.dispatchedByAccountId = dispatchedByAccountId;
            this.messageId = messageId;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Long getDispatchedByAccountId() { return dispatchedByAccountId; }
        public void setDispatchedByAccountId(Long dispatchedByAccountId) { this.dispatchedByAccountId = dispatchedByAccountId; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
