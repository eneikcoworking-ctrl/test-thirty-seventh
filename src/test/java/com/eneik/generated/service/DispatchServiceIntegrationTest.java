package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.OutboundDispatchRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
public class DispatchServiceIntegrationTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private OutboundDispatchRepository outboundDispatchRepository;

    private TgAccount account1;
    private TgAccount account2;
    private Campaign campaign;

    @BeforeEach
    public void setUp() {
        outboundDispatchRepository.deleteAll();
        tgAccountRepository.deleteAll();
        campaignRepository.deleteAll();

        // Setup accounts
        account1 = new TgAccount();
        account1.setPhoneNumber("+12345678901");
        account1.setStatus("Active");
        account1 = tgAccountRepository.save(account1);

        account2 = new TgAccount();
        account2.setPhoneNumber("+12345678902");
        account2.setStatus("Active");
        account2 = tgAccountRepository.save(account2);

        // Setup campaign
        campaign = new Campaign(UUID.randomUUID().toString(), "Test Campaign", "Hello!");
        campaign = campaignRepository.save(campaign);
    }

    @Test
    public void testDispatchesAreTrackedAndLimitEnforced() {
        // Configure daily limit to 2 for easier testing
        dispatchService.setDailyLimit(2);

        // First dispatch should succeed
        String msgId1 = dispatchService.dispatchCampaignMessage(account1, campaign, 100001L, "Message 1");
        assertNotNull(msgId1);
        assertTrue(msgId1.startsWith("tg_msg_"));

        // Second dispatch should succeed
        String msgId2 = dispatchService.dispatchCampaignMessage(account1, campaign, 100002L, "Message 2");
        assertNotNull(msgId2);

        // The count of dispatches for account1 should be 2
        long count = outboundDispatchRepository.countByTgAccountAndDispatchedAtAfter(
                account1, OffsetDateTime.now().minusHours(24)
        );
        assertEquals(2, count);

        // Third dispatch should fail (reaches limit of 2)
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            dispatchService.dispatchCampaignMessage(account1, campaign, 100003L, "Message 3");
        });
        assertTrue(ex.getMessage().contains("Daily dispatch limit reached"));

        // Verify account2 is unaffected and can still dispatch
        String msgIdOther = dispatchService.dispatchCampaignMessage(account2, campaign, 100004L, "Message other");
        assertNotNull(msgIdOther);
    }

    @Test
    public void testHistoricDispatchesOutsideWindowAreExcluded() {
        dispatchService.setDailyLimit(2);

        // Record a dispatch from 25 hours ago
        OutboundDispatch oldDispatch = new OutboundDispatch(
                account1,
                campaign.getId(),
                OffsetDateTime.now().minusHours(25)
        );
        outboundDispatchRepository.save(oldDispatch);

        // Record a dispatch from just now
        dispatchService.dispatchCampaignMessage(account1, campaign, 100001L, "Message 1");

        // The active count in last 24 hours should be 1 (excluding the one from 25 hours ago)
        long count = outboundDispatchRepository.countByTgAccountAndDispatchedAtAfter(
                account1, OffsetDateTime.now().minusHours(24)
        );
        assertEquals(1, count);

        // We can still send one more message
        String msgId = dispatchService.dispatchCampaignMessage(account1, campaign, 100002L, "Message 2");
        assertNotNull(msgId);

        // Next one should trigger the limit
        assertThrows(IllegalStateException.class, () -> {
            dispatchService.dispatchCampaignMessage(account1, campaign, 100003L, "Message 3");
        });
    }
}
