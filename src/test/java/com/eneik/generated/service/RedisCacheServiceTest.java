package com.eneik.generated.service;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.InboxService;
import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.repository.DialogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.cache.type=simple")
public class RedisCacheServiceTest {

    @Autowired
    private InboxService inboxService;

    @Autowired
    private DialogService dialogService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    public void setup() {
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().flushAll();
            } catch (Exception ignored) {}
        }

        conversationRepository.deleteAll();
        dialogRepository.deleteAll();

        // Clear caches
        cacheManager.getCache("conversations").clear();
        cacheManager.getCache("messages").clear();
        cacheManager.getCache("dialogs").clear();
    }

    @Test
    public void testConversationsCache_HitAndEviction() {
        OffsetDateTime now = OffsetDateTime.now();
        Conversation c = new Conversation(
                UUID.randomUUID().toString(),
                123456L,
                "Cache Test Lead",
                "cache_lead",
                "+12345",
                "ACTIVE",
                null,
                now,
                now
        );
        conversationRepository.save(c);

        // First call - queries db and populates cache
        Page<Conversation> firstCall = inboxService.getConversations("ALL", null, 0, 20);
        assertEquals(1, firstCall.getContent().size());

        // Delete from database directly
        conversationRepository.delete(c);

        // Second call - should return from cache! (even though it's deleted from db)
        Page<Conversation> secondCall = inboxService.getConversations("ALL", null, 0, 20);
        assertEquals(1, secondCall.getContent().size()); // Cache hit!

        // Re-save so receiveLeadMessage can find it
        conversationRepository.save(c);

        // Perform mutation to trigger eviction
        inboxService.receiveLeadMessage(c.getId(), "Hello!");

        // Delete from database again
        conversationRepository.delete(c);

        // Third call - cache is evicted, should query db and return empty since we deleted it again
        Page<Conversation> thirdCall = inboxService.getConversations("ALL", null, 0, 20);
        assertEquals(0, thirdCall.getContent().size()); // Cache evicted!
    }

    @Test
    public void testDialogsCache_HitAndEviction() {
        Dialog d = new Dialog("dialog_cache_chat", AiState.ACTIVE);
        Dialog saved = dialogRepository.save(d);

        // First call - populates cache
        Optional<Dialog> firstCall = dialogService.findDialogById(saved.getId());
        assertTrue(firstCall.isPresent());

        // Delete from database directly
        dialogRepository.delete(saved);

        // Second call - should return from cache!
        Optional<Dialog> secondCall = dialogService.findDialogById(saved.getId());
        assertTrue(secondCall.isPresent()); // Cache hit!

        // Re-save so handleStopTrigger can find it
        dialogRepository.save(saved);

        // Perform mutation
        dialogService.handleStopTrigger("dialog_cache_chat", AiState.PAUSED);

        // Delete from database again
        dialogRepository.delete(saved);

        // Third call - cache is evicted, should query db and return empty
        Optional<Dialog> thirdCall = dialogService.findDialogById(saved.getId());
        assertFalse(thirdCall.isPresent()); // Cache evicted!
    }
}
