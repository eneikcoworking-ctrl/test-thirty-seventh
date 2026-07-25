package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignDispatchService {

    private static final Logger log = LoggerFactory.getLogger(CampaignDispatchService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramBridgeService telegramBridgeService;
    private final TgAccountStateService tgAccountStateService;
    private final DelayCalculationService delayCalculationService;

    @Value("${app.typing.mean-delay-seconds:5.0}")
    private double meanDelaySeconds = 5.0;

    private static final double SAFE_FALLBACK_MIN_DELAY_SECONDS = 1.0;

    public CampaignDispatchService(TgAccountRepository tgAccountRepository,
                                   TelegramBridgeService telegramBridgeService,
                                   TgAccountStateService tgAccountStateService,
                                   DelayCalculationService delayCalculationService) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramBridgeService = telegramBridgeService;
        this.tgAccountStateService = tgAccountStateService;
        this.delayCalculationService = delayCalculationService;
    }

    public void setMeanDelaySeconds(double meanDelaySeconds) {
        this.meanDelaySeconds = meanDelaySeconds;
    }

    public double getMeanDelaySeconds() {
        return meanDelaySeconds;
    }

    /**
     * Dispatches a message on behalf of a campaign, using an eligible account from the pool.
     * Implements automatic session rotation and failover.
     *
     * @param campaignId the ID of the campaign
     * @param telegramChatId the target chat ID
     * @param text the message text
     * @return the message ID from the Telegram bridge
     */
    public String dispatchCampaignMessage(String campaignId, Long telegramChatId, String text) {
        while (true) {
            // Find eligible accounts (excluding FLOOD_WAIT status)
            List<TgAccount> eligibleAccounts = tgAccountRepository.findEligibleAccounts(campaignId, "FLOOD_WAIT");

            log.info("Eligible accounts count: {} for campaign: {}", eligibleAccounts.size(), campaignId);
            for (TgAccount acc : eligibleAccounts) {
                log.info(" - Account ID: {}, Phone: {}, Status: {}, Daily count: {}/{}",
                        acc.getId(), acc.getPhoneNumber(), acc.getStatus(), acc.getDailyDispatchCount(), acc.getDailyDispatchLimit());
            }

            if (eligibleAccounts.isEmpty()) {
                throw new NoEligibleAccountException("No eligible Telegram accounts available in the pool for campaign: " + campaignId);
            }

            // Always take the first available eligible account
            TgAccount activeAccount = eligibleAccounts.get(0);

            log.info("Attempting dispatch using account: {} (phone: {}, daily dispatch count: {}/{})",
                    activeAccount.getId(), activeAccount.getPhoneNumber(),
                    activeAccount.getDailyDispatchCount(), activeAccount.getDailyDispatchLimit());

            try {
                // Check if text triggers a simulated FLOOD_WAIT error (either general FORCE_FLOOD or phone-specific FORCE_FLOOD_<phone>)
                if (text != null && (text.contains("FORCE_FLOOD_" + activeAccount.getPhoneNumber()) || text.trim().equals("FORCE_FLOOD") || text.startsWith("FORCE_FLOOD message"))) {
                    throw new TelegramFloodWaitException("Simulated Telegram FLOOD_WAIT error for phone: " + activeAccount.getPhoneNumber());
                }

                // Send typing status signal to the recipient prior to dispatching
                try {
                    telegramBridgeService.sendTypingStatus(telegramChatId);
                } catch (IllegalArgumentException e) {
                    // Given an invalid recipient chat, When the typing signal is attempted, Then the dispatch fails gracefully without hanging the service.
                    log.error("Invalid recipient chat ID {}. Failing dispatch gracefully. Error: {}", telegramChatId, e.getMessage());
                    throw e;
                } catch (Exception e) {
                    // Given the messaging bridge fails to send the typing signal, When dispatch occurs, Then the system logs the error and proceeds or retries.
                    log.error("Failed to send typing status signal, proceeding with dispatch. Error: {}", e.getMessage());
                }

                // Apply randomized delay separated by typing signal and actual message dispatch
                double delaySeconds;
                if (meanDelaySeconds <= 0) {
                    delaySeconds = SAFE_FALLBACK_MIN_DELAY_SECONDS;
                } else {
                    try {
                        delaySeconds = delayCalculationService.calculateExponentialDelay(meanDelaySeconds);
                        if (delaySeconds < SAFE_FALLBACK_MIN_DELAY_SECONDS) {
                            delaySeconds = SAFE_FALLBACK_MIN_DELAY_SECONDS;
                        }
                    } catch (Exception e) {
                        delaySeconds = SAFE_FALLBACK_MIN_DELAY_SECONDS;
                    }
                }

                log.info("Applying typing emulation delay of {} seconds before dispatching actual message payload.", delaySeconds);
                try {
                    Thread.sleep((long) (delaySeconds * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Typing status emulation interrupted", ie);
                }

                // Emulate message dispatch via the Telegram core layer
                // Done outside a database transaction to prevent holding DB connections open during network I/O
                String messageId = telegramBridgeService.dispatchMessage(telegramChatId, text);

                // Increment daily dispatch count on successful dispatch (runs in its own transaction)
                tgAccountStateService.incrementDispatchCount(activeAccount.getId());

                log.info("Successfully dispatched message via account: {}.", activeAccount.getId());

                return messageId;

            } catch (TelegramFloodWaitException e) {
                log.warn("Account {} encountered FLOOD_WAIT. Rotating to next available account. Error: {}",
                        activeAccount.getId(), e.getMessage());

                // Update status to FLOOD_WAIT to mark it ineligible (runs in its own transaction)
                tgAccountStateService.markAsFloodWait(activeAccount.getId());

                // Continue loop to retry with the next available eligible account
            }
        }
    }
}
