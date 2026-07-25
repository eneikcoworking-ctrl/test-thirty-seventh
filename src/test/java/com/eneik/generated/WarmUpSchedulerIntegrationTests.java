package com.eneik.generated;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.model.Account;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.repository.WarmUpCycleRepository;
import com.eneik.generated.service.WarmUpSchedulerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Background Warm-up Action Scheduler.
 * Note: Transactional annotation is NOT used on the class level to ensure compatibility with Propagation.REQUIRES_NEW.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class WarmUpSchedulerIntegrationTests {

    @Autowired
    private WarmUpSchedulerService warmUpSchedulerService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private WarmUpCycleRepository warmUpCycleRepository;

    @BeforeEach
    public void setUp() {
        tearDown();
    }

    @AfterEach
    public void tearDown() {
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();
        warmUpCycleRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    public void testPositiveScenarioDispatchesAction() {
        // Given (Positive): a Telegram account with warm-up status 'IN_PROGRESS'
        Account account = new Account(
                "tg_positive_123",
                "+111222333",
                OffsetDateTime.now(),
                "IN_PROGRESS",
                1.5
        );
        account = accountRepository.save(account);

        // When the background job triggers
        warmUpSchedulerService.runWarmUpActions();

        // Then it successfully dispatches an automated action and increases the trust score
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals("IN_PROGRESS", updatedAccount.getWarmUpStatus());
        assertEquals(1.6, updatedAccount.getTrustScore(), 0.001);
    }

    @Test
    public void testNegativeScenarioDoesNotDispatchForOtherStatuses() {
        // Given (Negative 1): accounts not in IN_PROGRESS status
        Account accountNotStarted = new Account(
                "tg_ns_456",
                "+444555666",
                OffsetDateTime.now(),
                "NOT_STARTED",
                1.0
        );
        accountNotStarted = accountRepository.save(accountNotStarted);

        Account accountCompleted = new Account(
                "tg_c_789",
                "+777888999",
                OffsetDateTime.now(),
                "COMPLETED",
                2.5
        );
        accountCompleted = accountRepository.save(accountCompleted);

        // When the background job runs
        warmUpSchedulerService.runWarmUpActions();

        // Then no automated actions are dispatched for them and trust scores are unchanged
        Account updatedNotStarted = accountRepository.findById(accountNotStarted.getId()).orElseThrow();
        assertEquals(1.0, updatedNotStarted.getTrustScore(), 0.001);

        Account updatedCompleted = accountRepository.findById(accountCompleted.getId()).orElseThrow();
        assertEquals(2.5, updatedCompleted.getTrustScore(), 0.001);
    }

    @Test
    public void testNegativeScenarioHandlesInvalidProxyGracefully() {
        // Given (Negative 2): an account with invalid proxy credentials
        Account account = new Account(
                "tg_invalid_proxy",
                "+1234512345",
                OffsetDateTime.now(),
                "IN_PROGRESS",
                2.0
        );
        account = accountRepository.save(account);

        // Create an invalid proxy that fails connectivity check
        Proxy proxy = new Proxy();
        proxy.setIpAddress("fail_invalid_ip");
        proxy.setPort(9999);
        proxy.setProtocol("HTTP");
        proxy = proxyRepository.save(proxy);

        // Associate with TgAccount via phone number
        TgAccount tgAccount = new TgAccount();
        tgAccount.setPhoneNumber("+1234512345");
        tgAccount.setStatus("Active");
        tgAccount.setProxy(proxy);
        tgAccountRepository.save(tgAccount);

        // When the job runs and fails to connect
        assertDoesNotThrow(() -> {
            warmUpSchedulerService.runWarmUpActions();
        });

        // Then it logs the failure without crashing the scheduler, and the account is skipped (trust score unchanged)
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(2.0, updatedAccount.getTrustScore(), 0.001);
    }

    @Test
    public void testEdgeCaseScenarioProcessesInBatches() {
        // Given (Edge Case): multiple accounts in IN_PROGRESS status
        for (int i = 1; i <= 5; i++) {
            Account account = new Account(
                    "tg_batch_" + i,
                    "+999000" + i,
                    OffsetDateTime.now(),
                    "IN_PROGRESS",
                    1.0
            );
            accountRepository.save(account);
        }

        // Configure a small batch size to test batching/pagination
        int originalBatchSize = warmUpSchedulerService.getBatchSize();
        warmUpSchedulerService.setBatchSize(2);

        try {
            // When the scheduler runs
            warmUpSchedulerService.runWarmUpActions();

            // Then it processes all accounts successfully across multiple pages
            List<Account> accounts = accountRepository.findAll();
            assertEquals(5, accounts.size());
            for (Account account : accounts) {
                assertEquals(1.1, account.getTrustScore(), 0.001);
            }
        } finally {
            // Restore batch size
            warmUpSchedulerService.setBatchSize(originalBatchSize);
        }
    }
}
