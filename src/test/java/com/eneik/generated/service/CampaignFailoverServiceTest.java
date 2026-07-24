package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.dto.CampaignDispatchRequest;
import com.eneik.generated.leadgen.service.TelegramBridgeService;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.service.CampaignFailoverService.DispatchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CampaignFailoverServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignFailoverService campaignFailoverService;

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
        Campaign campaign = new Campaign(campaignId, "Failover Test Campaign", null);
        campaignRepository.save(campaign);
    }

    @Test
    public void testFloodWaitRotation() {
        // Given a campaign running across a pool of connected Telegram accounts
        TgAccount acc1 = new TgAccount();
        acc1.setPhoneNumber("+1111111111");
        acc1.setStatus("Active");
        acc1.setCampaignId(campaignId);
        acc1.setDailyDispatchLimit(10);
        acc1.setDailyDispatchCount(0);
        acc1 = tgAccountRepository.save(acc1);

        TgAccount acc2 = new TgAccount();
        acc2.setPhoneNumber("+2222222222");
        acc2.setStatus("Active");
        acc2.setCampaignId(campaignId);
        acc2.setDailyDispatchLimit(10);
        acc2.setDailyDispatchCount(0);
        acc2 = tgAccountRepository.save(acc2);

        // Mock the first account encountering a FLOOD_WAIT error
        // The second account succeeds
        when(telegramBridgeService.dispatchMessage(anyLong(), anyString()))
                .thenThrow(new RuntimeException("FLOOD_WAIT: Rate limit hit! Wait 60 seconds."))
                .thenReturn("successful_msg_from_acc2");

        // When a dispatch is triggered
        DispatchResult result = campaignFailoverService.dispatchWithFailover(campaignId, 12345L, "Hello, user!");

        // Then:
        // 1. The message is successfully sent
        assertTrue(result.isSuccess());
        assertEquals("successful_msg_from_acc2", result.getMessageId());
        assertEquals(acc2.getId(), result.getDispatchedByAccountId());

        // 2. The active account (acc1) is marked as FLOOD_WAIT
        TgAccount updatedAcc1 = tgAccountRepository.findById(acc1.getId()).orElseThrow();
        assertEquals("FLOOD_WAIT", updatedAcc1.getStatus());

        // 3. The second account (acc2) has its daily_dispatch_count incremented
        TgAccount updatedAcc2 = tgAccountRepository.findById(acc2.getId()).orElseThrow();
        assertEquals(1, updatedAcc2.getDailyDispatchCount());
        assertEquals("Active", updatedAcc2.getStatus());
    }

    @Test
    public void testDailyLimitSeamlessShift() {
        // Given an active account that reached its daily limit
        TgAccount acc1 = new TgAccount();
        acc1.setPhoneNumber("+1111111111");
        acc1.setStatus("Active");
        acc1.setCampaignId(campaignId);
        acc1.setDailyDispatchLimit(3);
        acc1.setDailyDispatchCount(3); // Limit reached!
        acc1 = tgAccountRepository.save(acc1);

        // And a second account that is still available
        TgAccount acc2 = new TgAccount();
        acc2.setPhoneNumber("+2222222222");
        acc2.setStatus("Active");
        acc2.setCampaignId(campaignId);
        acc2.setDailyDispatchLimit(5);
        acc2.setDailyDispatchCount(1); // Eligible
        acc2 = tgAccountRepository.save(acc2);

        // Mock success dispatch
        when(telegramBridgeService.dispatchMessage(anyLong(), anyString()))
                .thenReturn("msg_from_acc2");

        // When a dispatch is triggered
        DispatchResult result = campaignFailoverService.dispatchWithFailover(campaignId, 98765L, "Test daily limit shift");

        // Then:
        // 1. The task must seamlessly shift to the next available account
        assertTrue(result.isSuccess());
        assertEquals("msg_from_acc2", result.getMessageId());
        assertEquals(acc2.getId(), result.getDispatchedByAccountId());

        // 2. The daily count of acc2 is incremented
        TgAccount updatedAcc2 = tgAccountRepository.findById(acc2.getId()).orElseThrow();
        assertEquals(2, updatedAcc2.getDailyDispatchCount());

        // 3. The daily count of acc1 remains the same
        TgAccount updatedAcc1 = tgAccountRepository.findById(acc1.getId()).orElseThrow();
        assertEquals(3, updatedAcc1.getDailyDispatchCount());
    }

    @Test
    public void testNoEligibleAccountsException() {
        // Given no eligible accounts
        TgAccount acc1 = new TgAccount();
        acc1.setPhoneNumber("+1111111111");
        acc1.setStatus("FLOOD_WAIT"); // not eligible
        acc1.setCampaignId(campaignId);
        acc1.setDailyDispatchLimit(5);
        acc1.setDailyDispatchCount(0);
        tgAccountRepository.save(acc1);

        assertThrows(IllegalStateException.class, () -> {
            campaignFailoverService.dispatchWithFailover(campaignId, 12345L, "Failing dispatch");
        });
    }

    @Test
    public void testDispatchHttpEndpointSuccessAndFailover() throws Exception {
        // Given a campaign running across two accounts
        TgAccount acc1 = new TgAccount();
        acc1.setPhoneNumber("+1111111111");
        acc1.setStatus("Active");
        acc1.setCampaignId(campaignId);
        acc1.setDailyDispatchLimit(10);
        acc1.setDailyDispatchCount(0);
        acc1 = tgAccountRepository.save(acc1);

        TgAccount acc2 = new TgAccount();
        acc2.setPhoneNumber("+2222222222");
        acc2.setStatus("Active");
        acc2.setCampaignId(campaignId);
        acc2.setDailyDispatchLimit(10);
        acc2.setDailyDispatchCount(0);
        acc2 = tgAccountRepository.save(acc2);

        // Mock first dispatch to fail with FLOOD_WAIT, and the second to succeed
        when(telegramBridgeService.dispatchMessage(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Error: FLOOD_WAIT on first account"))
                .thenReturn("http_success_message_id");

        CampaignDispatchRequest request = new CampaignDispatchRequest(55555L, "Test HTTP message dispatch");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messageId").value("http_success_message_id"))
                .andExpect(jsonPath("$.dispatchedByAccountId").value(acc2.getId()));

        // Also check DB updates
        assertEquals("FLOOD_WAIT", tgAccountRepository.findById(acc1.getId()).orElseThrow().getStatus());
        assertEquals(1, tgAccountRepository.findById(acc2.getId()).orElseThrow().getDailyDispatchCount());
    }

    @Test
    public void testDispatchHttpEndpointNoEligibleAccounts() throws Exception {
        CampaignDispatchRequest request = new CampaignDispatchRequest(55555L, "No accounts");
        ObjectMapper mapper = new ObjectMapper();

        mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NO_ELIGIBLE_ACCOUNTS"));
    }
}
