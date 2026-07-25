package com.eneik.generated.leadgen;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class MessagesCacheLoadTest {

    @Autowired
    private InboxService inboxService;

    @Autowired
    private ConversationRepository conversationRepository;

    @SpyBean
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        conversationMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        if (cacheManager != null) {
            Cache messagesCache = cacheManager.getCache("messages");
            if (messagesCache != null) {
                messagesCache.clear();
            }
        }
    }

    @Test
    public void testHighConcurrencyMessageLoad_PreventsCacheStampede() throws Exception {
        // Given an active conversation with messages
        String conversationId = UUID.randomUUID().toString();
        Conversation conversation = new Conversation(
                conversationId,
                123456L,
                "Concurrent User",
                "concurrent_u",
                "+123456789",
                "ACTIVE",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        conversationRepository.save(conversation);

        ConversationMessage msg = new ConversationMessage(
                UUID.randomUUID().toString(),
                conversationId,
                "Test message for cache load",
                "LEAD",
                OffsetDateTime.now(),
                "Concurrent User"
        );
        conversationMessageRepository.save(msg);

        // Reset spy invocations
        Mockito.clearInvocations(conversationMessageRepository);

        // Define concurrency parameters
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<Future<List<ConversationMessage>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await(); // wait for the signal to start concurrently
                try {
                    return inboxService.getMessages(conversationId, 10, null);
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        // When all threads start concurrently
        startLatch.countDown();
        boolean finishedInTime = finishLatch.await(10, TimeUnit.SECONDS);
        assertTrue(finishedInTime, "All threads should finish execution");

        // Verify results
        for (Future<List<ConversationMessage>> future : futures) {
            List<ConversationMessage> result = future.get();
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test message for cache load", result.get(0).getText());
        }

        // Then database query must be made EXACTLY once, demonstrating successful cache-stampede prevention
        verify(conversationMessageRepository, times(1))
                .findByConversationId(eq(conversationId), any());

        executorService.shutdown();
    }

    @Test
    public void testCacheEvictionThresholdsAndFallbackBehavior() {
        // Test BoundedConcurrentMap eviction/boundary behavior directly to verify threshold enforcement
        int maxEntries = 10;
        com.eneik.generated.CacheConfig.BoundedConcurrentMap<String, String> boundedMap =
                new com.eneik.generated.CacheConfig.BoundedConcurrentMap<>(maxEntries);

        // Add elements up to the threshold
        for (int i = 0; i < maxEntries; i++) {
            boundedMap.put("key" + i, "val" + i);
        }
        assertEquals(maxEntries, boundedMap.size(), "Map should be full at max capacity");

        // Adding one more exceeding threshold should trigger eviction (clear() for bounded map fallback configuration)
        boundedMap.put("exceedingKey", "exceedingVal");
        assertTrue(boundedMap.size() <= maxEntries, "Map must enforce its boundary ceiling and evict old entries");
        assertNull(boundedMap.get("key0"), "Older keys should be cleared/evicted");
        assertEquals("exceedingVal", boundedMap.get("exceedingKey"));
    }

    @Test
    public void testFailSafeCacheEvictionPropagation_HappyPath() {
        Cache mockDelegate = Mockito.mock(Cache.class);
        Cache mockFallback = Mockito.mock(Cache.class);
        Mockito.when(mockDelegate.getName()).thenReturn("messages");

        com.eneik.generated.CacheConfig.FailSafeCache failSafeCache =
                new com.eneik.generated.CacheConfig.FailSafeCache(mockDelegate, mockFallback);

        // Trigger evict on happy path
        failSafeCache.evict("testKey");
        verify(mockDelegate, times(1)).evict("testKey");
        // fallback evict should NOT be called because delegate succeeded
        verify(mockFallback, Mockito.never()).evict("testKey");

        // Trigger clear on happy path
        failSafeCache.clear();
        verify(mockDelegate, times(1)).clear();
        verify(mockFallback, Mockito.never()).clear();
    }

    @Test
    public void testFailSafeCacheEvictionPropagation_FallbackPath() {
        Cache mockDelegate = Mockito.mock(Cache.class);
        Cache mockFallback = Mockito.mock(Cache.class);
        Mockito.when(mockDelegate.getName()).thenReturn("messages");

        // Force delegate to throw exception during evict/clear to trigger fallback
        Mockito.doThrow(new RuntimeException("Delegate error")).when(mockDelegate).evict(any());
        Mockito.doThrow(new RuntimeException("Delegate error")).when(mockDelegate).clear();

        com.eneik.generated.CacheConfig.FailSafeCache failSafeCache =
                new com.eneik.generated.CacheConfig.FailSafeCache(mockDelegate, mockFallback);

        // Trigger evict on fallback path
        failSafeCache.evict("testKey");
        verify(mockDelegate, times(1)).evict("testKey");
        verify(mockFallback, times(1)).evict("testKey");

        // Trigger clear on fallback path
        failSafeCache.clear();
        verify(mockDelegate, times(1)).clear();
        verify(mockFallback, times(1)).clear();
    }
}
