package com.eneik.generated;

import com.eneik.generated.model.Account;
import com.eneik.generated.model.WarmUpCycle;
import com.eneik.generated.model.WarmUpTask;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.WarmUpCycleRepository;
import com.eneik.generated.repository.WarmUpTaskRepository;
import com.eneik.generated.service.DelayService;
import com.eneik.generated.service.WarmUpScheduler;
import com.eneik.generated.service.WarmUpService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    @Autowired
    private WarmUpTaskRepository warmUpTaskRepository;

    @Autowired
    private WarmUpScheduler warmUpScheduler;

    @MockBean
    private DelayService delayService;

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

    @Test
    public void testWarmUpTaskDirectExecution() {
        // Given a registered account
        Account account = warmUpService.registerAccount("tg_task_direct", "+111222333");
        double initialScore = account.getTrustScore();

        // And a pending warm-up task
        WarmUpTask task = warmUpService.createWarmUpTask(account.getId(), "@telegram_channel");
        assertNotNull(task.getId());
        assertEquals("PENDING", task.getStatus());
        assertFalse(task.isSubscribed());
        assertFalse(task.isMarkedAsRead());

        // When executed
        long expectedDelay = 500L;
        WarmUpTask executedTask = warmUpService.executeWarmUpTask(task.getId(), expectedDelay);

        // Then the account subscribes to the channel and marks it as read
        assertEquals("COMPLETED", executedTask.getStatus());
        assertTrue(executedTask.isSubscribed());
        assertTrue(executedTask.isMarkedAsRead());
        assertNotNull(executedTask.getExecutedAt());
        assertEquals(expectedDelay, executedTask.getAppliedDelayMs());

        // And the account trust score increases
        Account updatedAccount = accountRepository.findById(account.getId()).get();
        assertEquals(initialScore + 0.1, updatedAccount.getTrustScore(), 0.001);
    }

    @Test
    public void testWarmUpSchedulerUtilizesExponentialDelays() throws InterruptedException {
        // Given an account and three pending tasks
        Account account = warmUpService.registerAccount("tg_scheduler_delay", "+444555666");
        warmUpService.createWarmUpTask(account.getId(), "@chan_1");
        warmUpService.createWarmUpTask(account.getId(), "@chan_2");
        warmUpService.createWarmUpTask(account.getId(), "@chan_3");

        // When scheduler is configured with baseDelayMs and multiplier
        long baseDelay = 100L;
        double multiplier = 3.0;
        warmUpScheduler.setBaseDelayMs(baseDelay);
        warmUpScheduler.setMultiplier(multiplier);

        // And running the schedule
        warmUpScheduler.runPendingWarmUpTasks();

        // Then it correctly utilizes the exponential delays:
        // Task 0: 100 * 3^0 = 100 ms
        // Task 1: 100 * 3^1 = 300 ms
        // Task 2: 100 * 3^2 = 900 ms
        verify(delayService, times(1)).sleep(100L);
        verify(delayService, times(1)).sleep(300L);
        verify(delayService, times(1)).sleep(900L);

        // And all tasks are completed with correct subscription/read states and delay records
        List<WarmUpTask> tasks = warmUpTaskRepository.findByAccountId(account.getId());
        assertEquals(3, tasks.size());
        for (WarmUpTask task : tasks) {
            assertEquals("COMPLETED", task.getStatus());
            assertTrue(task.isSubscribed());
            assertTrue(task.isMarkedAsRead());
        }

        assertEquals(100L, tasks.get(0).getAppliedDelayMs());
        assertEquals(300L, tasks.get(1).getAppliedDelayMs());
        assertEquals(900L, tasks.get(2).getAppliedDelayMs());
    }
}
