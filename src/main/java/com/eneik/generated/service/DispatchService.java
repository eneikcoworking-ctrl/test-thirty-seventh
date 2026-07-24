package com.eneik.generated.service;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.OutboundDispatchRepository;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class DispatchService {

    private final OutboundDispatchRepository outboundDispatchRepository;
    private final TelegramBridgeService telegramBridgeService;
    private int dailyLimit = 20;

    public DispatchService(OutboundDispatchRepository outboundDispatchRepository,
                           TelegramBridgeService telegramBridgeService) {
        this.outboundDispatchRepository = outboundDispatchRepository;
        this.telegramBridgeService = telegramBridgeService;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    @Transactional
    public String dispatchCampaignMessage(TgAccount account, Campaign campaign, Long telegramChatId, String text) {
        if (account == null) {
            throw new IllegalArgumentException("TgAccount must not be null");
        }

        // Check daily limit (contacts contacted in the last 24 hours)
        long sentCount = outboundDispatchRepository.countByTgAccountAndDispatchedAtAfter(
                account, OffsetDateTime.now().minusHours(24)
        );

        if (sentCount >= dailyLimit) {
            throw new IllegalStateException("Daily dispatch limit reached for account: " + account.getPhoneNumber());
        }

        // Dispatch via Telegram bridge layer
        String messageId = telegramBridgeService.dispatchMessage(telegramChatId, text);

        // Record the outbound dispatch
        OutboundDispatch dispatch = new OutboundDispatch(
                account,
                campaign != null ? campaign.getId() : null,
                OffsetDateTime.now()
        );
        outboundDispatchRepository.save(dispatch);

        return messageId;
    }
}
