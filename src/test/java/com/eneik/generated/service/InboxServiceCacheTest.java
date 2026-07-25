package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.repository.ConversationMessageRepository;
import com.eneik.generated.leadgen.repository.ConversationRepository;
import com.eneik.generated.leadgen.service.InboxService;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class InboxServiceCacheTest {

    private static final String CONV_ID = "conv-123";
    private static final String CACHE_NAME = "messages";
    private static final String SENDER_TYPE_LEAD = "LEAD";
    private static final String SENDER_NAME_LEAD = "Alice";
    private static final String MSG_BEFORE_ID = "msg-999";
    private static final String TEXT_MANUAL = "Manual Rep Message";
    private static final String TEXT_LEAD = "Hello from Lead";
    private static final int LIMIT = 50;

    private static final String TEST_KEY = "test-key";
    private static final String TEST_VAL = "test-val";
    private static final String REDIS_ERR_MSG = "Redis connection lost";

    @Autowired
    private InboxService inboxService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ConversationRepository conversationRepository;

    @MockBean
    private ConversationMessageRepository conversationMessageRepository;

    @MockBean
    private TelegramBridgeService telegramBridgeService;

    private Conversation conversation;
    private List<ConversationMessage> mockMessages;

    @BeforeEach
    public void setUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }

        conversation = new Conversation();
        conversation.setId(CONV_ID);
        conversation.setTelegramChatId(12345L);
        conversation.setStatus("ACTIVE");
        conversation.setLeadName(SENDER_NAME_LEAD);

        ConversationMessage msg = new ConversationMessage();
        msg.setId("msg-1");
        msg.setConversationId(CONV_ID);
        msg.setText("Initial message");
        msg.setSenderType(SENDER_TYPE_LEAD);
        msg.setSentAt(OffsetDateTime.now());
        msg.setSenderName(SENDER_NAME_LEAD);

        mockMessages = Collections.singletonList(msg);
    }

    @Test
    public void testGetMessagesCachesWhenBeforeMessageIdIsNull() {
        when(conversationMessageRepository.findByConversationId(eq(CONV_ID), any(Pageable.class)))
                .thenReturn(mockMessages);

        List<ConversationMessage> res1 = inboxService.getMessages(CONV_ID, LIMIT, null);
        assertEquals(1, res1.size());

        List<ConversationMessage> res2 = inboxService.getMessages(CONV_ID, LIMIT, null);
        assertEquals(1, res2.size());

        verify(conversationMessageRepository, times(1))
                .findByConversationId(eq(CONV_ID), any(Pageable.class));
    }

    @Test
    public void testGetMessagesDoesNotCacheWhenBeforeMessageIdIsNotNull() {
        when(conversationMessageRepository.findByConversationIdAndIdLessThan(eq(CONV_ID), eq(MSG_BEFORE_ID), any(Pageable.class)))
                .thenReturn(mockMessages);

        inboxService.getMessages(CONV_ID, LIMIT, MSG_BEFORE_ID);
        inboxService.getMessages(CONV_ID, LIMIT, MSG_BEFORE_ID);

        verify(conversationMessageRepository, times(2))
                .findByConversationIdAndIdLessThan(eq(CONV_ID), eq(MSG_BEFORE_ID), any(Pageable.class));
    }

    @Test
    public void testSendManualMessageEvictsCache() {
        when(conversationMessageRepository.findByConversationId(eq(CONV_ID), any(Pageable.class)))
                .thenReturn(mockMessages);
        when(conversationRepository.findById(eq(CONV_ID))).thenReturn(Optional.of(conversation));

        ConversationMessage manualMsg = new ConversationMessage();
        manualMsg.setId("msg-manual");
        manualMsg.setConversationId(CONV_ID);
        manualMsg.setText(TEXT_MANUAL);
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenReturn(manualMsg);

        inboxService.getMessages(CONV_ID, LIMIT, null);
        inboxService.sendManualMessage(CONV_ID, TEXT_MANUAL);
        inboxService.getMessages(CONV_ID, LIMIT, null);

        verify(conversationMessageRepository, times(2))
                .findByConversationId(eq(CONV_ID), any(Pageable.class));
    }

    @Test
    public void testReceiveLeadMessageEvictsCache() {
        when(conversationMessageRepository.findByConversationId(eq(CONV_ID), any(Pageable.class)))
                .thenReturn(mockMessages);
        when(conversationRepository.findById(eq(CONV_ID))).thenReturn(Optional.of(conversation));

        ConversationMessage leadMsg = new ConversationMessage();
        leadMsg.setId("msg-lead");
        leadMsg.setConversationId(CONV_ID);
        leadMsg.setText(TEXT_LEAD);
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenReturn(leadMsg);

        inboxService.getMessages(CONV_ID, LIMIT, null);
        inboxService.receiveLeadMessage(CONV_ID, TEXT_LEAD);
        inboxService.getMessages(CONV_ID, LIMIT, null);

        verify(conversationMessageRepository, times(2))
                .findByConversationId(eq(CONV_ID), any(Pageable.class));
    }

    @Test
    public void testFailSafeCacheManagerFallsBackOnRedisException() {
        Cache mockPrimaryCache = mock(Cache.class);
        doThrow(new RuntimeException(REDIS_ERR_MSG)).when(mockPrimaryCache).put(any(), any());
        doThrow(new RuntimeException(REDIS_ERR_MSG)).when(mockPrimaryCache).get(any());

        com.eneik.generated.config.FailSafeCache failSafeCache =
                new com.eneik.generated.config.FailSafeCache(mockPrimaryCache, CACHE_NAME);

        failSafeCache.put(TEST_KEY, TEST_VAL);

        Cache.ValueWrapper wrapper = failSafeCache.get(TEST_KEY);
        assertNotNull(wrapper);
        assertEquals(TEST_VAL, wrapper.get());
    }
}
