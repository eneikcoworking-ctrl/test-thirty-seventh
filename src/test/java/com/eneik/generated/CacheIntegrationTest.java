package com.eneik.generated;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.config.CacheConstants;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.service.InboxService;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.service.CampaignService;
import com.eneik.generated.service.DialogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private DialogService dialogService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @BeforeEach
    public void clearCaches() {
        conversationMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Test
    public void testCampaignCachingAndEviction() {
        // Create a campaign
        String id = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(id, "Test Cache Campaign", "Rules");
        campaignService.saveCampaign(campaign);

        // Fetch to populate cache
        assertTrue(campaignService.getCampaign(id).isPresent());

        // Check cache is populated
        Cache cache = cacheManager.getCache(CacheConstants.CACHE_CAMPAIGN_BY_ID);
        assertNotNull(cache);
        Cache.ValueWrapper wrapper = cache.get(id);
        assertNotNull(wrapper);
        assertNotNull(wrapper.get());

        // Fetch list to populate campaigns list cache
        assertFalse(campaignService.getAllCampaigns().isEmpty());
        Cache listCache = cacheManager.getCache(CacheConstants.CACHE_CAMPAIGNS);
        assertNotNull(listCache);
        assertNotNull(listCache.get("all"));

        // Save campaign again (or update) to trigger eviction
        campaignService.saveCampaign(campaign);

        // Check cache is evicted
        assertNull(cache.get(id));
        assertNull(listCache.get("all"));
    }

    @Test
    public void testConversationsAndMessagesCachingAndEviction() {
        String convId = UUID.randomUUID().toString();
        Conversation conv = new Conversation(
                convId,
                999999L,
                "Cache Test Lead",
                "cache_test_l",
                "+12345",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conv);

        // Populate conversations cache
        assertFalse(inboxService.getConversations("ALL", null, 0, 10).isEmpty());

        Cache convCache = cacheManager.getCache(CacheConstants.CACHE_CONVERSATIONS);
        assertNotNull(convCache);
        String convKey = "ALL__0_10";
        assertNotNull(convCache.get(convKey));

        // Populate messages cache with correct limit=50 to satisfy caching condition
        inboxService.getMessages(convId, 50, null);

        Cache msgCache = cacheManager.getCache(CacheConstants.CACHE_MESSAGES);
        assertNotNull(msgCache);
        assertNotNull(msgCache.get(convId));

        // Trigger eviction by sending manual message (evicts messages cache dynamically but preserves conversations list cache to allow TTL expiration)
        inboxService.sendManualMessage(convId, "Rep manual response");

        // Check evicted
        assertNull(msgCache.get(convId));
        assertNotNull(convCache.get(convKey)); // conversations list caching remains active until TTL expiration
    }
}
