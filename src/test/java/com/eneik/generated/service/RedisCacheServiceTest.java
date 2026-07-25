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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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

    @BeforeEach
    public void setup() {
        conversationRepository.deleteAll();
        dialogRepository.deleteAll();
        // Clear caches
        Cache conversationsCache = cacheManager.getCache("conversations");
        if (conversationsCache != null) {
            conversationsCache.clear();
        }
        Cache messagesCache = cacheManager.getCache("messages");
        if (messagesCache != null) {
            messagesCache.clear();
        }
        Cache dialogsCache = cacheManager.getCache("dialogs");
        if (dialogsCache != null) {
            dialogsCache.clear();
        }
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

        // First call - populates cache
        Page<Conversation> firstCall = inboxService.getConversations("ALL", null, 0, 20);
        assertNotNull(firstCall);

        Cache conversationsCache = cacheManager.getCache("conversations");
        assertNotNull(conversationsCache);

        // Verify cache contains the key
        String expectedKey = "status_ALL_agent_null_page_0_limit_20";
        Cache.ValueWrapper cachedValue = conversationsCache.get(expectedKey);
        assertNotNull(cachedValue);

        // Perform mutation to trigger eviction
        inboxService.receiveLeadMessage(c.getId(), "Hello!");

        // Assert cache is evicted
        assertNull(conversationsCache.get(expectedKey));
    }

    @Test
    public void testDialogsCache_HitAndEviction() {
        Dialog d = new Dialog("dialog_cache_chat", AiState.ACTIVE);
        Dialog saved = dialogRepository.save(d);

        // First call - populates cache
        Optional<Dialog> firstCall = dialogService.findDialogById(saved.getId());
        assertTrue(firstCall.isPresent());

        Cache dialogsCache = cacheManager.getCache("dialogs");
        assertNotNull(dialogsCache);

        String expectedKey = "id_" + saved.getId();
        assertNotNull(dialogsCache.get(expectedKey));

        // Perform mutation
        dialogService.handleStopTrigger("dialog_cache_chat", AiState.PAUSED);

        // Assert cache is evicted
        assertNull(dialogsCache.get(expectedKey));
    }
}
