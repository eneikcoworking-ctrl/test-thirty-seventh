package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.repository.OutboundDispatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class DispatchServiceIntegrationTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private OutboundDispatchRepository outboundDispatchRepository;

    private TgAccount activeAccount;
    private Campaign campaign;

    @BeforeEach
    public void setUp() {
        outboundDispatchRepository.deleteAll();
        tgAccountRepository.deleteAll();
        campaignRepository.deleteAll();

        // Save a standard active Telegram account
        activeAccount = new TgAccount();
        activeAccount.setPhoneNumber("+1234567890");
        activeAccount.setStatus("Active");
        activeAccount = tgAccountRepository.save(activeAccount);

        // Save a standard Campaign
        String campaignId = UUID.randomUUID().toString();
        campaign = new Campaign(campaignId, "Test Campaign", "Hello {name}");
        campaign = campaignRepository.save(campaign);
    }

    @Test
    public void testSuccessfulDispatchAndTracking() {
        // Given an active Telegram account and a campaign
        Long accountId = activeAccount.getId();
        String campaignId = campaign.getId();

        // When a message is dispatched
        OutboundDispatch dispatch = dispatchService.dispatchMessage(
                accountId,
                campaignId,
                11111L,
                "@prospect_user",
                "Hello @prospect_user!"
        );

        // Then the dispatch is tracked and returned
        assertNotNull(dispatch);
        assertNotNull(dispatch.getId());
        assertEquals(accountId, dispatch.getTgAccount().getId());
        assertEquals(campaignId, dispatch.getCampaignId());
        assertEquals("@prospect_user", dispatch.getRecipientPhoneOrUsername());
        assertNotNull(dispatch.getDispatchedAt());

        // Assert database entry exists
        long count = outboundDispatchRepository.count();
        assertEquals(1, count);

        Optional<OutboundDispatch> persisted = outboundDispatchRepository.findById(dispatch.getId());
        assertTrue(persisted.isPresent());
        assertEquals("@prospect_user", persisted.get().getRecipientPhoneOrUsername());
    }

    @Test
    public void testDispatchHaltsWhenLimitIsReached() {
        // Given we set the daily limit to exactly 3 for testing
        dispatchService.setDailyLimit(3);

        Long accountId = activeAccount.getId();
        String campaignId = campaign.getId();

        // When 3 messages are dispatched successfully
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 11111L, "@user1", "Message 1"));
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 22222L, "@user2", "Message 2"));
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 33333L, "@user3", "Message 3"));

        // Then current count is 3
        long currentCount = outboundDispatchRepository.countByTgAccountIdAndDispatchedAtAfter(accountId, LocalDateTime.now().minusHours(24));
        assertEquals(3, currentCount);

        // When the 4th dispatch is attempted, it must throw IllegalStateException and halt dispatches
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            dispatchService.dispatchMessage(accountId, campaignId, 44444L, "@user4", "Message 4");
        });

        assertEquals("Daily outbound message limit reached for account: " + accountId, exception.getMessage());

        // And the 4th dispatch is NOT saved to the database (count remains 3)
        assertEquals(3, outboundDispatchRepository.count());
    }

    @Test
    public void testSliding24HourWindowAllowsOlderDispatches() {
        // Given we set the daily limit to 2
        dispatchService.setDailyLimit(2);

        Long accountId = activeAccount.getId();
        String campaignId = campaign.getId();

        // Given an older dispatch that happened 25 hours ago
        OutboundDispatch oldDispatch = new OutboundDispatch(activeAccount, campaignId, "@old_user");
        oldDispatch.setDispatchedAt(LocalDateTime.now().minusHours(25));
        outboundDispatchRepository.save(oldDispatch);

        // And another dispatch that happened 2 hours ago (within 24 hours window)
        OutboundDispatch recentDispatch = new OutboundDispatch(activeAccount, campaignId, "@recent_user");
        recentDispatch.setDispatchedAt(LocalDateTime.now().minusHours(2));
        outboundDispatchRepository.save(recentDispatch);

        // The current active count in 24h is 1 (recentDispatch only)
        long currentCount = outboundDispatchRepository.countByTgAccountIdAndDispatchedAtAfter(accountId, LocalDateTime.now().minusHours(24));
        assertEquals(1, currentCount);

        // We can successfully dispatch 1 more message (reaching the limit of 2)
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 55555L, "@user5", "Message 5"));

        // A subsequent dispatch will exceed the limit and thus throws an exception
        assertThrows(IllegalStateException.class, () -> {
            dispatchService.dispatchMessage(accountId, campaignId, 66666L, "@user6", "Message 6");
        });
    }

    @Test
    public void testCannotDispatchWithInactiveAccount() {
        // Given a Telegram account that is not active
        TgAccount inactiveAccount = new TgAccount();
        inactiveAccount.setPhoneNumber("+19999999999");
        inactiveAccount.setStatus("Banned");
        inactiveAccount = tgAccountRepository.save(inactiveAccount);

        Long inactiveId = inactiveAccount.getId();
        String campaignId = campaign.getId();

        // When trying to dispatch, an IllegalStateException must be thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            dispatchService.dispatchMessage(inactiveId, campaignId, 77777L, "@user7", "Message 7");
        });

        assertTrue(exception.getMessage().contains("Telegram Account is not active"));
    }
}
