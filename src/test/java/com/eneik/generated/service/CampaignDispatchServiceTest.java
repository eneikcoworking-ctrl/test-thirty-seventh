package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class CampaignDispatchServiceTest {

    @Autowired
    private CampaignDispatchService campaignDispatchService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @MockBean
    private TelegramBridgeService telegramBridgeService;

    private String campaignId;

    @BeforeEach
    public void setUp() {
        tgAccountRepository.deleteAll();
        campaignRepository.deleteAll();

        // Create a test campaign
        campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(campaignId, "Dispatch Test Campaign", null);
        campaignRepository.save(campaign);

        // Add an active TgAccount
        TgAccount acc = new TgAccount();
        acc.setPhoneNumber("+1234567890");
        acc.setStatus("Active");
        acc.setCampaignId(campaignId);
        acc.setDailyDispatchLimit(10);
        acc.setDailyDispatchCount(0);
        tgAccountRepository.save(acc);

        // Set typing delay to a small but positive value (e.g. 1.0) so tests run fast
        campaignDispatchService.setMeanDelaySeconds(1.0);
    }

    @Test
    public void testSuccessfulDispatchSendsTypingSignalAndDelay() {
        // Given
        Long chatId = 12345L;
        String text = "Hello Prospect";
        when(telegramBridgeService.dispatchMessage(eq(chatId), eq(text))).thenReturn("msg_123");

        // When
        String messageId = campaignDispatchService.dispatchCampaignMessage(campaignId, chatId, text);

        // Then
        assertEquals("msg_123", messageId);

        // Verify that typing status is sent PRIOR to dispatching message
        InOrder inOrder = inOrder(telegramBridgeService);
        inOrder.verify(telegramBridgeService).sendTypingStatus(chatId);
        inOrder.verify(telegramBridgeService).dispatchMessage(chatId, text);
    }

    @Test
    public void testTypingSignalFailureLogsAndProceeds() {
        // Given: typing status fails, but dispatchMessage succeeds
        Long chatId = 12345L;
        String text = "Hello Prospect";
        doThrow(new RuntimeException("Bridge connection timeout")).when(telegramBridgeService).sendTypingStatus(anyLong());
        when(telegramBridgeService.dispatchMessage(eq(chatId), eq(text))).thenReturn("msg_proceed");

        // When
        String messageId = campaignDispatchService.dispatchCampaignMessage(campaignId, chatId, text);

        // Then: the message is still sent successfully
        assertEquals("msg_proceed", messageId);
        verify(telegramBridgeService).sendTypingStatus(chatId);
        verify(telegramBridgeService).dispatchMessage(chatId, text);
    }

    @Test
    public void testInvalidRecipientChatFailsGracefully() {
        // Given: invalid chat ID <= 0, which makes sendTypingStatus throw IllegalArgumentException
        Long invalidChatId = -100L;
        String text = "Hello Prospect";
        doThrow(new IllegalArgumentException("Invalid recipient chat ID: " + invalidChatId))
                .when(telegramBridgeService).sendTypingStatus(eq(invalidChatId));

        // When & Then: dispatch fails gracefully without getting stuck in infinite loop
        assertThrows(IllegalArgumentException.class, () -> {
            campaignDispatchService.dispatchCampaignMessage(campaignId, invalidChatId, text);
        });

        verify(telegramBridgeService).sendTypingStatus(invalidChatId);
        verify(telegramBridgeService, never()).dispatchMessage(anyLong(), anyString());
    }

    @Test
    public void testSafeFallbackMinimumDelayOnMisconfiguration() {
        // Given: misconfigured mean delay (0.0 or negative)
        campaignDispatchService.setMeanDelaySeconds(0.0);

        Long chatId = 12345L;
        String text = "Hello Prospect";
        when(telegramBridgeService.dispatchMessage(eq(chatId), eq(text))).thenReturn("msg_fallback");

        long startTime = System.currentTimeMillis();

        // When
        String messageId = campaignDispatchService.dispatchCampaignMessage(campaignId, chatId, text);

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;

        // Then: the message is sent and a fallback delay of at least 1.0 second (1000ms) is applied
        assertEquals("msg_fallback", messageId);
        assertTrue(elapsed >= 950, "Should apply safe fallback delay of at least 1 second, elapsed: " + elapsed);
    }
}
