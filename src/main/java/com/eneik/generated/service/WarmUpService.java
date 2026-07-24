package com.eneik.generated.service;

import com.eneik.generated.model.Account;
import com.eneik.generated.model.WarmUpCycle;
import com.eneik.generated.model.WarmUpTask;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.WarmUpCycleRepository;
import com.eneik.generated.repository.WarmUpTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class WarmUpService {

    private final AccountRepository accountRepository;
    private final WarmUpCycleRepository warmUpCycleRepository;
    private final WarmUpTaskRepository warmUpTaskRepository;

    public WarmUpService(AccountRepository accountRepository,
                         WarmUpCycleRepository warmUpCycleRepository,
                         WarmUpTaskRepository warmUpTaskRepository) {
        this.accountRepository = accountRepository;
        this.warmUpCycleRepository = warmUpCycleRepository;
        this.warmUpTaskRepository = warmUpTaskRepository;
    }

    @Transactional
    public Account registerAccount(String telegramId, String phoneNumber) {
        Account account = new Account(
                telegramId,
                phoneNumber,
                OffsetDateTime.now(),
                "NOT_STARTED",
                1.0 // Initial starting trust score
        );
        return accountRepository.save(account);
    }

    @Transactional
    public WarmUpCycle startWarmUpCycle(Long accountId, double targetScoreIncrease) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        account.setWarmUpStatus("IN_PROGRESS");
        accountRepository.save(account);

        WarmUpCycle cycle = new WarmUpCycle(
                account,
                OffsetDateTime.now(),
                "ACTIVE",
                targetScoreIncrease
        );
        return warmUpCycleRepository.save(cycle);
    }

    @Transactional
    public WarmUpCycle completeWarmUpCycle(Long cycleId) {
        WarmUpCycle cycle = warmUpCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Warm-up cycle not found: " + cycleId));

        if (!"ACTIVE".equals(cycle.getStatus())) {
            throw new IllegalStateException("Warm-up cycle is already completed or not active");
        }

        cycle.setStatus("COMPLETED");
        cycle.setCompletedAt(OffsetDateTime.now());

        Account account = cycle.getAccount();
        double currentScore = account.getTrustScore();
        double updatedScore = currentScore + cycle.getTargetScoreIncrease();
        account.setTrustScore(updatedScore);
        account.setWarmUpStatus("COMPLETED");

        accountRepository.save(account);
        return warmUpCycleRepository.save(cycle);
    }

    @Transactional
    public WarmUpTask createWarmUpTask(Long accountId, String channelUsername) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        WarmUpTask task = new WarmUpTask(account, channelUsername, "PENDING");
        return warmUpTaskRepository.save(task);
    }

    @Transactional
    public WarmUpTask executeWarmUpTask(Long taskId, long appliedDelayMs) {
        WarmUpTask task = warmUpTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Warm-up task not found: " + taskId));

        if (!"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Task is already executed or not in PENDING state");
        }

        // Action: Subscribe to a channel and mark it as read
        task.setSubscribed(true);
        task.setMarkedAsRead(true);
        task.setStatus("COMPLETED");
        task.setExecutedAt(OffsetDateTime.now());
        task.setAppliedDelayMs(appliedDelayMs);

        // Maximize account lifespan and trust score: award slightly more trust score per task
        Account account = task.getAccount();
        account.setTrustScore(account.getTrustScore() + 0.1);
        accountRepository.save(account);

        return warmUpTaskRepository.save(task);
    }
}
