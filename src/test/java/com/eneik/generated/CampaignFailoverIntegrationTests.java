package com.eneik.generated;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.service.CampaignDispatchService;
import com.eneik.generated.service.NoEligibleAccountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class CampaignFailoverIntegrationTests {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private CampaignDispatchService campaignDispatchService;

    private Campaign campaign;
    private Proxy proxy;

    @BeforeEach
    public void setUp() {
        tgAccountRepository.deleteAll();
        campaignRepository.deleteAll();
        proxyRepository.deleteAll();

        // Save a campaign
        campaign = new Campaign(UUID.randomUUID().toString(), "Failover Campaign", "{Hello}");
        campaign = campaignRepository.save(campaign);

        // Save a proxy
        proxy = new Proxy();
        proxy.setIpAddress("127.0.0.1");
        proxy.setPort(1080);
        proxy.setProtocol("SOCKS5");
        proxy = proxyRepository.save(proxy);
    }

    @Test
    public void testSeamlessShiftWhenActiveAccountReachesDailyLimit() {
        // Given a campaign running across a pool of connected Telegram accounts:
        // Account A: almost reached daily limit (49 out of 50)
        TgAccount accountA = new TgAccount();
        accountA.setPhoneNumber("+10000000001");
        accountA.setStatus("Active");
        accountA.setProxy(proxy);
        accountA.setCampaignId(campaign.getId());
        accountA.setDailyDispatchCount(49);
        accountA.setDailyDispatchLimit(50);
        accountA.setCreatedAt(LocalDateTime.now().minusMonths(2));
        accountA = tgAccountRepository.save(accountA);

        // Account B: has remaining capacity
        TgAccount accountB = new TgAccount();
        accountB.setPhoneNumber("+10000000002");
        accountB.setStatus("Active");
        accountB.setProxy(proxy);
        accountB.setCampaignId(campaign.getId());
        accountB.setDailyDispatchCount(10);
        accountB.setDailyDispatchLimit(50);
        accountB.setCreatedAt(LocalDateTime.now().minusMonths(2));
        accountB = tgAccountRepository.save(accountB);

        // When a dispatch is triggered (First dispatch)
        String firstMsgId = campaignDispatchService.dispatchCampaignMessage(campaign.getId(), 12345L, "First dispatch");
        assertNotNull(firstMsgId);

        // Then it should be dispatched by Account A (which is the first eligible account)
        TgAccount updatedA = tgAccountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(50, updatedA.getDailyDispatchCount(), "Account A should have reached its limit of 50");

        // When a second dispatch is triggered (A has now reached its daily limit)
        String secondMsgId = campaignDispatchService.dispatchCampaignMessage(campaign.getId(), 12345L, "Second dispatch");
        assertNotNull(secondMsgId);

        // Then the task must seamlessly shift to the next available account (Account B)
        TgAccount updatedB = tgAccountRepository.findById(accountB.getId()).orElseThrow();
        assertEquals(11, updatedB.getDailyDispatchCount(), "Account B should have dispatched the message and incremented its count to 11");

        // Verify Account A is no longer selected as it has reached limit
        updatedA = tgAccountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(50, updatedA.getDailyDispatchCount());
    }

    @Test
    public void testAutomaticRotationOnFloodWaitError() {
        // Given a campaign running across a pool of connected Telegram accounts:
        // Account A: Active
        TgAccount accountA = new TgAccount();
        accountA.setPhoneNumber("+10000000003");
        accountA.setStatus("Active");
        accountA.setProxy(proxy);
        accountA.setCampaignId(campaign.getId());
        accountA.setDailyDispatchCount(0);
        accountA.setDailyDispatchLimit(50);
        accountA.setCreatedAt(LocalDateTime.now().minusMonths(2));
        accountA = tgAccountRepository.save(accountA);

        // Account B: Active
        TgAccount accountB = new TgAccount();
        accountB.setPhoneNumber("+10000000004");
        accountB.setStatus("Active");
        accountB.setProxy(proxy);
        accountB.setCampaignId(campaign.getId());
        accountB.setDailyDispatchCount(0);
        accountB.setDailyDispatchLimit(50);
        accountB.setCreatedAt(LocalDateTime.now().minusMonths(2));
        accountB = tgAccountRepository.save(accountB);

        // When the active account (A) encounters a FLOOD_WAIT error (simulated via FORCE_FLOOD_<phone> message text)
        String msgId = campaignDispatchService.dispatchCampaignMessage(campaign.getId(), 54321L, "Message with FORCE_FLOOD_+10000000003");
        assertNotNull(msgId);

        // Then Account A should be marked as FLOOD_WAIT in database
        TgAccount updatedA = tgAccountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals("FLOOD_WAIT", updatedA.getStatus(), "Account A should have its status updated to FLOOD_WAIT");
        assertEquals(0, updatedA.getDailyDispatchCount(), "Account A should not have incremented its dispatch count on failure");

        // And the task must rotate to the next eligible account (Account B) and successfully complete
        TgAccount updatedB = tgAccountRepository.findById(accountB.getId()).orElseThrow();
        assertEquals("Active", updatedB.getStatus());
        assertEquals(1, updatedB.getDailyDispatchCount(), "Account B should have completed the rotated task and incremented its count to 1");
    }

    @Test
    public void testPoolExhaustionThrowsNoEligibleAccountException() {
        // Given a campaign running across a pool of connected Telegram accounts:
        // Account A: Active
        TgAccount accountA = new TgAccount();
        accountA.setPhoneNumber("+10000000005");
        accountA.setStatus("Active");
        accountA.setProxy(proxy);
        accountA.setCampaignId(campaign.getId());
        accountA.setDailyDispatchCount(0);
        accountA.setDailyDispatchLimit(50);
        accountA.setCreatedAt(LocalDateTime.now().minusMonths(2));
        accountA = tgAccountRepository.save(accountA);

        // When the active account encounters FLOOD_WAIT but there is no other eligible account in the pool
        assertThrows(NoEligibleAccountException.class, () -> {
            campaignDispatchService.dispatchCampaignMessage(campaign.getId(), 54321L, "FORCE_FLOOD message");
        }, "Should throw NoEligibleAccountException when the entire pool is exhausted due to FLOOD_WAIT errors");

        // Verify Account A is indeed marked FLOOD_WAIT
        TgAccount updatedA = tgAccountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals("FLOOD_WAIT", updatedA.getStatus());
    }
}
