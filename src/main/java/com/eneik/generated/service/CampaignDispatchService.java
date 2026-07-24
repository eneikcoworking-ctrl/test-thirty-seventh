package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignDispatchService {

    private static final Logger log = LoggerFactory.getLogger(CampaignDispatchService.class);

    private final TgAccountRepository tgAccountRepository;
    private final TelegramBridgeService telegramBridgeService;
    private final TgAccountStateService tgAccountStateService;

    public CampaignDispatchService(TgAccountRepository tgAccountRepository,
                                   TelegramBridgeService telegramBridgeService,
                                   TgAccountStateService tgAccountStateService) {
        this.tgAccountRepository = tgAccountRepository;
        this.telegramBridgeService = telegramBridgeService;
        this.tgAccountStateService = tgAccountStateService;
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
            // Find eligible accounts (Active, and daily_dispatch_count < daily_dispatch_limit)
            List<TgAccount> eligibleAccounts = tgAccountRepository.findEligibleAccounts(campaignId, "Active");

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
