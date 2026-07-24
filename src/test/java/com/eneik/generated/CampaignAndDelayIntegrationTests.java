package com.eneik.generated;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.dto.ActionDelayResponse;
import com.eneik.generated.dto.ActionRequest;
import com.eneik.generated.dto.CampaignAssignmentRequest;
import com.eneik.generated.dto.CampaignAssignmentRequest.AccountType;
import com.eneik.generated.model.Account;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.service.DelayCalculationService;
import com.eneik.generated.service.CampaignAssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {Application.class, CampaignAndDelayIntegrationTests.Config.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class CampaignAndDelayIntegrationTests {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        public DelayCalculationService fixedDelayCalculationService() {
            return new DelayCalculationService(new Random(42L));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DelayCalculationService delayCalculationService;

    @Autowired
    private CampaignAssignmentService campaignAssignmentService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Test
    public void testDelayDistributionComputation() {
        // Given a mean delay of 150 seconds
        double meanDelay = 150.0;

        // When generating multiple samples
        int samplesCount = 1000;
        double sum = 0;
        for (int i = 0; i < samplesCount; i++) {
            double delay = delayCalculationService.calculateExponentialDelay(meanDelay);
            assertTrue(delay >= 0, "Delay must be non-negative");
            sum += delay;
        }

        // Then the sample mean should be reasonably close to the configured mean
        double sampleMean = sum / samplesCount;
        double tolerance = meanDelay * 0.15; // 15% tolerance
        assertEquals(meanDelay, sampleMean, tolerance,
                String.format("Sample mean %.2f was not close to expected mean %.2f", sampleMean, meanDelay));
    }

    @Test
    public void testDelayInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            delayCalculationService.calculateExponentialDelay(-10.0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            delayCalculationService.calculateExponentialDelay(0.0);
        });
    }

    @Test
    public void testCampaignAssignmentWithOlderAndYoungerTgAccounts() {
        UUID campaignId = UUID.randomUUID();

        // 1. Create a proxy
        Proxy proxy = new Proxy();
        proxy.setIpAddress("127.0.0.1");
        proxy.setPort(8080);
        proxy.setProtocol("SOCKS5");
        proxy = proxyRepository.save(proxy);

        // 2. Create an older TgAccount (2 months old)
        TgAccount oldTgAccount = new TgAccount();
        oldTgAccount.setPhoneNumber("+12223334444");
        oldTgAccount.setStatus("Active");
        oldTgAccount.setProxy(proxy);
        oldTgAccount.setCreatedAt(LocalDateTime.now().minusMonths(2));
        oldTgAccount = tgAccountRepository.save(oldTgAccount);

        // 3. Create a younger TgAccount (2 weeks old)
        TgAccount youngTgAccount = new TgAccount();
        youngTgAccount.setPhoneNumber("+15556667777");
        youngTgAccount.setStatus("Active");
        youngTgAccount.setProxy(proxy);
        youngTgAccount.setCreatedAt(LocalDateTime.now().minusWeeks(2));
        youngTgAccount = tgAccountRepository.save(youngTgAccount);

        // Verify that old TgAccount is accepted
        boolean successOld = campaignAssignmentService.assignCampaign(campaignId, oldTgAccount.getId(), AccountType.TG_ACCOUNT);
        assertTrue(successOld);

        // Verify that young TgAccount is rejected
        Long youngId = youngTgAccount.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            campaignAssignmentService.assignCampaign(campaignId, youngId, AccountType.TG_ACCOUNT);
        });
        assertTrue(ex.getMessage().contains("under 1 month old"));
    }

    @Test
    public void testCampaignAssignmentWithOlderAndYoungerWarmUpAccounts() {
        UUID campaignId = UUID.randomUUID();

        // 1. Create an older Account (1.5 months old)
        Account oldAccount = new Account(
                "tg_old_user",
                "+19998887777",
                OffsetDateTime.now().minusDays(45),
                "COMPLETED",
                5.0
        );
        oldAccount = accountRepository.save(oldAccount);

        // 2. Create a younger Account (5 days old)
        Account youngAccount = new Account(
                "tg_young_user",
                "+11112223333",
                OffsetDateTime.now().minusDays(5),
                "NOT_STARTED",
                1.0
        );
        youngAccount = accountRepository.save(youngAccount);

        // Verify old Account is accepted
        boolean successOld = campaignAssignmentService.assignCampaign(campaignId, oldAccount.getId(), AccountType.WARM_UP_ACCOUNT);
        assertTrue(successOld);

        // Verify young Account is rejected
        Long youngId = youngAccount.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            campaignAssignmentService.assignCampaign(campaignId, youngId, AccountType.WARM_UP_ACCOUNT);
        });
        assertTrue(ex.getMessage().contains("under 1 month old"));
    }

    @Test
    public void testSchedulerControllerApi() throws Exception {
        ActionRequest request = new ActionRequest("TYPING", 120.0);

        MvcResult result = mockMvc.perform(post("/api/v1/scheduler/delay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ActionDelayResponse response = objectMapper.readValue(content, ActionDelayResponse.class);

        assertNotNull(response.getDelaySeconds(), "Delay seconds should not be null");
        assertTrue(response.getDelaySeconds() >= 0.0, "Delay seconds should be non-negative");
    }

    @Test
    public void testSchedulerControllerApiInvalidInput() throws Exception {
        ActionRequest request = new ActionRequest("TYPING", -5.0);

        mockMvc.perform(post("/api/v1/scheduler/delay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("meanDelaySeconds must be positive")));
    }

    @Test
    public void testCampaignControllerApiSuccessAndFailure() throws Exception {
        UUID campaignId = UUID.randomUUID();

        // Create older TgAccount (50 days old)
        Proxy proxy = new Proxy();
        proxy.setIpAddress("127.0.0.1");
        proxy.setPort(8080);
        proxy.setProtocol("HTTP");
        proxy = proxyRepository.save(proxy);

        TgAccount tgAccount = new TgAccount();
        tgAccount.setPhoneNumber("+17778889999");
        tgAccount.setStatus("Active");
        tgAccount.setProxy(proxy);
        tgAccount.setCreatedAt(LocalDateTime.now().minusDays(50));
        tgAccount = tgAccountRepository.save(tgAccount);

        // Request with older eligible account
        CampaignAssignmentRequest request = new CampaignAssignmentRequest(campaignId, tgAccount.getId(), AccountType.TG_ACCOUNT);

        mockMvc.perform(post("/api/v1/campaigns/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.campaignId", is(campaignId.toString())))
                .andExpect(jsonPath("$.accountId", is(tgAccount.getId().intValue())))
                .andExpect(jsonPath("$.accountType", is("TG_ACCOUNT")))
                .andExpect(jsonPath("$.status", is("ASSIGNED")));

        // Create younger TgAccount (3 days old)
        TgAccount youngTgAccount = new TgAccount();
        youngTgAccount.setPhoneNumber("+13334445555");
        youngTgAccount.setStatus("Active");
        youngTgAccount.setProxy(proxy);
        youngTgAccount.setCreatedAt(LocalDateTime.now().minusDays(3));
        youngTgAccount = tgAccountRepository.save(youngTgAccount);

        CampaignAssignmentRequest requestYoung = new CampaignAssignmentRequest(campaignId, youngTgAccount.getId(), AccountType.TG_ACCOUNT);

        mockMvc.perform(post("/api/v1/campaigns/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestYoung)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_ACCOUNT_AGE")))
                .andExpect(jsonPath("$.message", containsString("under 1 month old")));
    }

    @Test
    public void testCampaignControllerApiNotFound() throws Exception {
        UUID campaignId = UUID.randomUUID();
        CampaignAssignmentRequest request = new CampaignAssignmentRequest(campaignId, 99999L, AccountType.TG_ACCOUNT);

        mockMvc.perform(post("/api/v1/campaigns/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("TgAccount not found with ID: 99999")));
    }
}
