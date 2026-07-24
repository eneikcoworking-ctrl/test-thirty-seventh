package com.eneik.generated.service;

import com.eneik.generated.model.Account;
import com.eneik.generated.model.WarmUpCycle;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.WarmUpCycleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class WarmUpService {

    private final AccountRepository accountRepository;
    private final WarmUpCycleRepository warmUpCycleRepository;

    public WarmUpService(AccountRepository accountRepository, WarmUpCycleRepository warmUpCycleRepository) {
        this.accountRepository = accountRepository;
        this.warmUpCycleRepository = warmUpCycleRepository;
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
}
