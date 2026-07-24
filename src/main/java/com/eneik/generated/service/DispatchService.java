package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.repository.OutboundDispatchRepository;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final TgAccountRepository tgAccountRepository;
    private final OutboundDispatchRepository outboundDispatchRepository;
    private final TelegramBridgeService telegramBridgeService;

    @Value("${app.rate-limiting.daily-limit:15}")
    private int dailyLimit = 15;

    public DispatchService(TgAccountRepository tgAccountRepository,
                           OutboundDispatchRepository outboundDispatchRepository,
                           TelegramBridgeService telegramBridgeService) {
        this.tgAccountRepository = tgAccountRepository;
        this.outboundDispatchRepository = outboundDispatchRepository;
        this.telegramBridgeService = telegramBridgeService;
    }

    /**
     * Attempts to dispatch a campaign message using a specific Telegram account.
     * Enforces strict daily message limits (sliding 24-hour window) per Telegram account.
     *
     * @param tgAccountId the ID of the Telegram account
     * @param campaignId the ID of the campaign task
     * @param telegramChatId the recipient's Telegram chat ID
     * @param recipientPhoneOrUsername the recipient's phone or username
     * @param text the message content
     * @return the persisted OutboundDispatch record
     * @throws IllegalStateException if the daily limit is reached or the account is invalid
     */
    public OutboundDispatch dispatchMessage(Long tgAccountId, String campaignId, Long telegramChatId, String recipientPhoneOrUsername, String text) {
        log.info("Attempting dispatch from Telegram Account: {} - Campaign: {} - Recipient: {}", tgAccountId, campaignId, recipientPhoneOrUsername);

        // 1. Retrieve and validate the Telegram Account
        TgAccount tgAccount = tgAccountRepository.findById(tgAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Telegram Account not found with ID: " + tgAccountId));

        if (!"Active".equalsIgnoreCase(tgAccount.getStatus())) {
            throw new IllegalStateException("Telegram Account is not active: Status is " + tgAccount.getStatus());
        }

        // 2. Enforce the rate limit (sliding 24-hour window check)
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        long currentCount = outboundDispatchRepository.countByTgAccountIdAndDispatchedAtAfter(tgAccountId, threshold);

        log.debug("Current outbound count for account {} in the last 24h: {} (Limit: {})", tgAccountId, currentCount, dailyLimit);

        boolean limitReached = currentCount >= dailyLimit || tgAccount.getDailyDispatchCount() >= tgAccount.getDailyDispatchLimit();

        if (limitReached) {
            log.info("Daily outbound message limit reached for account: {}. Attempting dynamic failover.", tgAccountId);

            TgAccount candidateAccount = null;
            if (campaignId != null && !campaignId.trim().isEmpty()) {
                List<TgAccount> campaignAccounts = tgAccountRepository.findByCampaignIdAndStatusIgnoreCaseOrderByIdAsc(campaignId, "Active");
                for (TgAccount candidate : campaignAccounts) {
                    if (candidate.getId().equals(tgAccountId)) {
                        continue;
                    }
                    long candidateCount = outboundDispatchRepository.countByTgAccountIdAndDispatchedAtAfter(candidate.getId(), threshold);
                    if (candidateCount < dailyLimit && candidate.getDailyDispatchCount() < candidate.getDailyDispatchLimit()) {
                        candidateAccount = candidate;
                        break;
                    }
                }
            }

            if (candidateAccount != null) {
                log.info("Failover transition: Switching dispatch task from account ID {} to available account ID {} for campaign {}", tgAccountId, candidateAccount.getId(), campaignId);
                tgAccount = candidateAccount;
            } else {
                log.warn("Rate limit reached for account {} and no other available accounts in campaign {}. Pausing dispatch gracefully.", tgAccountId, campaignId);
                return null;
            }
        }

        // 3. Dispatch the message via the Telegram bridge layer
        telegramBridgeService.dispatchMessage(telegramChatId, text);

        // 4. Update the account's daily dispatch count
        tgAccount.setDailyDispatchCount(tgAccount.getDailyDispatchCount() + 1);
        tgAccountRepository.save(tgAccount);

        // 5. Save and return the dispatch log record
        OutboundDispatch outboundDispatch = new OutboundDispatch(tgAccount, campaignId, recipientPhoneOrUsername);
        return outboundDispatchRepository.save(outboundDispatch);
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
}
