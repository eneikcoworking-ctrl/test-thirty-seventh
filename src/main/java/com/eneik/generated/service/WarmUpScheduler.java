package com.eneik.generated.service;

import com.eneik.generated.model.Account;
import com.eneik.generated.model.WarmUpTask;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.WarmUpTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarmUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarmUpScheduler.class);

    private final AccountRepository accountRepository;
    private final WarmUpTaskRepository warmUpTaskRepository;
    private final WarmUpService warmUpService;
    private final DelayService delayService;

    @Value("${warmup.schedule.base-delay-ms:1000}")
    private long baseDelayMs;

    @Value("${warmup.schedule.multiplier:2.0}")
    private double multiplier;

    public WarmUpScheduler(AccountRepository accountRepository,
                           WarmUpTaskRepository warmUpTaskRepository,
                           WarmUpService warmUpService,
                           DelayService delayService) {
        this.accountRepository = accountRepository;
        this.warmUpTaskRepository = warmUpTaskRepository;
        this.warmUpService = warmUpService;
        this.delayService = delayService;
    }

    /**
     * Periodically runs pending warm-up tasks for all accounts.
     * Can also be triggered manually or programmatically.
     */
    @Scheduled(fixedDelayString = "${warmup.schedule.fixed-delay-ms:60000}")
    public void runPendingWarmUpTasks() {
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            List<WarmUpTask> pendingTasks = warmUpTaskRepository.findByAccountIdAndStatus(account.getId(), "PENDING");
            for (int i = 0; i < pendingTasks.size(); i++) {
                WarmUpTask task = pendingTasks.get(i);

                // Exponential delay calculation: baseDelayMs * (multiplier^i)
                long delay = (long) (baseDelayMs * Math.pow(multiplier, i));

                try {
                    delayService.sleep(delay);
                    warmUpService.executeWarmUpTask(task.getId(), delay);
                } catch (InterruptedException e) {
                    log.error("Warm-up task execution interrupted for account {}: {}", account.getId(), e.getMessage(), e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Failed to execute warm-up task {} for account {}: {}", task.getId(), account.getId(), e.getMessage(), e);
                    task.setStatus("FAILED");
                    warmUpTaskRepository.save(task);
                }
            }
        }
    }

    // Getters and Setters for configuration properties to facilitate testing
    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
