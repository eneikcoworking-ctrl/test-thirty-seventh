package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.repository.OutboundDispatchRepository;
import com.eneik.generated.config.RedisConfig.RedisAvailabilityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {Application.class, RedisDispatchIntegrationTest.TestRedisConfig.class})
@ActiveProfiles("test")
public class RedisDispatchIntegrationTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private OutboundDispatchRepository outboundDispatchRepository;

    @Autowired
    private RedisQueueService redisQueueService;

    @Autowired
    private RedisRateLimiterService redisRateLimiterService;

    @Autowired
    private RedisAvailabilityChecker redisAvailabilityChecker;

    @Autowired
    private RedisTemplate<String, String> mockRedisTemplate;

    private TgAccount activeAccount;
    private Campaign campaign;

    @TestConfiguration
    public static class TestRedisConfig {

        @Bean
        @Primary
        public RedisConnectionFactory mockRedisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
            RedisConnection connection = mock(RedisConnection.class);
            when(factory.getConnection()).thenReturn(connection);
            return factory;
        }

        @Bean
        @Primary
        public RedisTemplate<String, String> mockRedisTemplate(RedisConnectionFactory factory) {
            RedisTemplate<String, String> template = mock(RedisTemplate.class);
            when(template.getConnectionFactory()).thenReturn(factory);
            return template;
        }
    }

    @BeforeEach
    public void setUp() {
        outboundDispatchRepository.deleteAll();
        tgAccountRepository.deleteAll();
        campaignRepository.deleteAll();

        // Reset the mock interactions before each test
        reset(mockRedisTemplate);

        // Manually inject Redis components since @Autowired(required=false) is used in DispatchService and we might want to ensure they are set
        dispatchService.setRedisQueueService(redisQueueService);
        dispatchService.setRedisRateLimiterService(redisRateLimiterService);
        dispatchService.setRedisAvailabilityChecker(redisAvailabilityChecker);

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
    public void testSuccessfulDispatchWhenRedisIsAvailable() {
        // Setup mocks for redisTemplate operations
        ListOperations<String, String> mockListOps = mock(ListOperations.class);
        ValueOperations<String, String> mockValOps = mock(ValueOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(mockListOps);
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValOps);
        when(mockValOps.increment(anyString())).thenReturn(1L);

        // Force Redis Availability to true for simulating the active state
        redisAvailabilityChecker.setAvailableOverride(true);

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

        // Then it should be tracked successfully and pushed to the Redis queue and incremented in Redis rate limiter
        assertNotNull(dispatch);
        assertEquals(accountId, dispatch.getTgAccount().getId());
        assertEquals(campaignId, dispatch.getCampaignId());

        // Verify that Redis was called to check rate limits, push to list, and increment count atomically
        verify(mockValOps, atLeastOnce()).get("rate:limit:account:" + accountId);
        verify(mockListOps, times(1)).rightPush(eq("campaign:dispatch:queue:" + campaignId), eq("@prospect_user::Hello @prospect_user!"));
        verify(mockValOps, times(1)).increment("rate:limit:account:" + accountId);
    }

    @Test
    public void testRedisRateLimiterEnforcement() {
        // Setup mocks
        ValueOperations<String, String> mockValOps = mock(ValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValOps);
        when(mockValOps.get(anyString())).thenReturn("3"); // simulating 3 counts in Redis (limit reached)

        // Force Redis Availability to true
        redisAvailabilityChecker.setAvailableOverride(true);

        Long accountId = activeAccount.getId();
        String campaignId = campaign.getId();

        dispatchService.setDailyLimit(3);

        // When dispatch is attempted on rate-limited account with no candidate failovers
        OutboundDispatch dispatch = dispatchService.dispatchMessage(
                accountId,
                campaignId,
                11111L,
                "@user_blocked",
                "Hello blocked"
        );

        // Then it should return null to pause gracefully
        assertNull(dispatch);
    }

    @Test
    public void testGracefulFallbackWhenRedisIsUnavailable() {
        // Force Redis Availability to false to simulate Redis being down
        redisAvailabilityChecker.setAvailableOverride(false);

        Long accountId = activeAccount.getId();
        String campaignId = campaign.getId();

        // Set daily limit to 2
        dispatchService.setDailyLimit(2);

        // Since Redis is down, we use DB-backed sliding 24h window
        // Dispatch 2 messages successfully (reaches the limit of 2)
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 11111L, "@user1", "Message 1"));
        assertNotNull(dispatchService.dispatchMessage(accountId, campaignId, 22222L, "@user2", "Message 2"));

        // A subsequent dispatch will exceed the limit and return null (gracefully pauses)
        OutboundDispatch result = dispatchService.dispatchMessage(accountId, campaignId, 33333L, "@user3", "Message 3");
        assertNull(result, "Should pause gracefully on DB fallback when Redis is unavailable");

        // Verify mock redis template was never interacted with for ops
        verifyNoInteractions(mockRedisTemplate);
    }
}
