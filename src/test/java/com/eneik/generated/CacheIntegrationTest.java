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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.SerializationFeature;

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
        Cache cache = cacheManager.getCache(CacheConstants.CAMPAIGN_BY_ID);
        assertNotNull(cache);
        System.out.println("Cache class: " + cache.getClass().getName());
        System.out.println("Cache native map: " + cache.getNativeCache());
        Cache.ValueWrapper wrapper = cache.get(id);
        if (wrapper == null) {
            System.out.println("Cache wrapper is null for id: " + id);
        } else {
            System.out.println("Cache wrapper holds: " + wrapper.get());
        }
        assertNotNull(wrapper);
        assertNotNull(wrapper.get());

        // Fetch list to populate campaigns list cache
        assertFalse(campaignService.getAllCampaigns().isEmpty());
        Cache listCache = cacheManager.getCache(CacheConstants.CAMPAIGNS);
        assertNotNull(listCache);
        Object campaignsKey = org.springframework.cache.interceptor.SimpleKey.EMPTY;
        assertNotNull(listCache.get(campaignsKey));

        // Save campaign again (or update) to trigger eviction
        campaignService.saveCampaign(campaign);

        // Check cache is evicted
        assertNull(cache.get(id));
        assertNull(listCache.get(campaignsKey));
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

        Cache convCache = cacheManager.getCache(CacheConstants.CONVERSATIONS);
        assertNotNull(convCache);
        Object convKey = new org.springframework.cache.interceptor.SimpleKey("ALL", null, 0, 10);
        assertNotNull(convCache.get(convKey));

        // Populate messages cache with correct limit=50 to satisfy caching condition
        inboxService.getMessages(convId, 50, null);

        Cache msgCache = cacheManager.getCache(CacheConstants.MESSAGES);
        assertNotNull(msgCache);
        assertNotNull(msgCache.get(convId));

        // Trigger eviction by sending manual message (evicts messages cache dynamically but preserves conversations list cache to allow TTL expiration)
        inboxService.sendManualMessage(convId, "Rep manual response");

        // Check evicted
        assertNull(msgCache.get(convId));
        assertNull(convCache.get(convKey)); // conversations list caching is evicted programmatically to reflect fresh CRM updates
    }

    @Test
    public void testPageSerializationAndDeserializationPolymorphism() throws Exception {
        // Create specialized ObjectMapper mirroring CacheConfig
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        objectMapper.registerModule(new com.eneik.generated.CacheConfig.PageModule());
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Given a page of conversations
        Conversation conv = new Conversation(
                "id123",
                12345L,
                "Polymorphic Lead",
                "poly_lead",
                "+999",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        java.util.List<Conversation> list = java.util.List.of(conv);
        Page<Conversation> page = new PageImpl<>(list, PageRequest.of(0, 10), 1);

        // When serialized
        String json = objectMapper.writeValueAsString(page);
        System.out.println("Serialized JSON: " + json);

        // Then deserialized
        Page<?> deserializedPage = objectMapper.readValue(json, Page.class);
        assertNotNull(deserializedPage);
        assertEquals(1, deserializedPage.getTotalElements());
        assertEquals(1, deserializedPage.getContent().size());

        Object firstItem = deserializedPage.getContent().get(0);
        // Verify type polymorphism preservation
        assertEquals(Conversation.class, firstItem.getClass(), "Type must be preserved as Conversation instead of LinkedHashMap");
    }
}
