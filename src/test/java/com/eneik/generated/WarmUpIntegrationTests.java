package com.eneik.generated;

import com.eneik.generated.model.Account;
import com.eneik.generated.model.WarmUpCycle;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.WarmUpCycleRepository;
import com.eneik.generated.service.WarmUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
public class WarmUpIntegrationTests {

    @Autowired
    private WarmUpService warmUpService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WarmUpCycleRepository warmUpCycleRepository;

    @Test
    public void testAccountOnboardingAndStateTracking() {
        // Given a new account
        String telegramId = "tg_123456";
        String phoneNumber = "+1234567890";

        // When inserted (registered)
        Account account = warmUpService.registerAccount(telegramId, phoneNumber);

        // Then its creation date and warm-up status are recorded
        assertNotNull(account.getId());
        assertEquals(telegramId, account.getTelegramId());
        assertEquals(phoneNumber, account.getPhoneNumber());
        assertNotNull(account.getCreatedAt());
        assertEquals("NOT_STARTED", account.getWarmUpStatus());
        assertEquals(1.0, account.getTrustScore(), 0.001);

        // Verify from repository
        Optional<Account> retrieved = accountRepository.findById(account.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("NOT_STARTED", retrieved.get().getWarmUpStatus());
        assertNotNull(retrieved.get().getCreatedAt());
    }

    @Test
    public void testWarmUpCycleCompletionAndTrustScoreUpdates() {
        // Given a new account and an active warm-up cycle
        Account account = warmUpService.registerAccount("tg_789", "+987654321");
        double initialTrustScore = account.getTrustScore();

        WarmUpCycle cycle = warmUpService.startWarmUpCycle(account.getId(), 2.5);
        assertNotNull(cycle.getId());
        assertEquals("ACTIVE", cycle.getStatus());

        // Verify state is now "IN_PROGRESS"
        Account inProgressAccount = accountRepository.findById(account.getId()).get();
        assertEquals("IN_PROGRESS", inProgressAccount.getWarmUpStatus());

        // When complete (completed)
        WarmUpCycle completedCycle = warmUpService.completeWarmUpCycle(cycle.getId());

        // Then the trust score updates and status of account/cycle changes
        assertEquals("COMPLETED", completedCycle.getStatus());
        assertNotNull(completedCycle.getCompletedAt());

        Account updatedAccount = accountRepository.findById(account.getId()).get();
        assertEquals("COMPLETED", updatedAccount.getWarmUpStatus());
        assertEquals(initialTrustScore + 2.5, updatedAccount.getTrustScore(), 0.001);
    }
}
