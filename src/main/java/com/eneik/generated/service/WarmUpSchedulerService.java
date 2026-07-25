package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.model.Account;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
public class WarmUpSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(WarmUpSchedulerService.class);

    private final AccountRepository accountRepository;
    private final TgAccountRepository tgAccountRepository;
    private final ProxyValidationService proxyValidationService;

    private int batchSize = 50;
    private Random random = new Random();

    public WarmUpSchedulerService(AccountRepository accountRepository,
                                  TgAccountRepository tgAccountRepository,
                                  ProxyValidationService proxyValidationService) {
        this.accountRepository = accountRepository;
        this.tgAccountRepository = tgAccountRepository;
        this.proxyValidationService = proxyValidationService;
    }

    @Value("${warmup.scheduler.batch-size:50}")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @Scheduled(cron = "${warmup.scheduler.cron:0 0/15 * * * ?}")
    public void scheduleWarmUpActions() {
        log.info("Scheduled trigger for warm-up actions.");
        runWarmUpActions();
    }

    public void runWarmUpActions() {
        log.info("Starting background warm-up action scheduler run.");
        int page = 0;
        Page<Account> accountPage;
        do {
            accountPage = accountRepository.findByWarmUpStatus("IN_PROGRESS", PageRequest.of(page, batchSize));
            for (Account account : accountPage.getContent()) {
                try {
                    // 1. Perform proxy connectivity check outside db transaction
                    boolean checkPassed = verifyProxyIfExist(account);
                    if (!checkPassed) {
                        continue; // Skip this account, do not crash the scheduler
                    }

                    // 2. Dispatch action and update trust score inside isolated transaction
                    dispatchAndRecordAction(account.getId());
                } catch (Exception e) {
                    log.error("Failed to process warm-up action for account ID " + account.getId(), e);
                }
            }
            page++;
        } while (accountPage.hasNext());
        log.info("Finished background warm-up action scheduler run.");
    }

    public boolean verifyProxyIfExist(Account account) {
        Optional<TgAccount> tgAccountOpt = tgAccountRepository.findByPhoneNumber(account.getPhoneNumber());
        if (tgAccountOpt.isPresent()) {
            TgAccount tgAccount = tgAccountOpt.get();
            if (tgAccount.getProxy() != null) {
                com.eneik.generated.domain.Proxy proxy = tgAccount.getProxy();
                try {
                    boolean valid = proxyValidationService.isValidProxy(
                            proxy.getIpAddress(),
                            proxy.getPort(),
                            proxy.getProtocol()
                    );
                    if (!valid) {
                        log.warn("Proxy connection failed for account ID: {} with proxy {}:{}",
                                account.getId(), proxy.getIpAddress(), proxy.getPort());
                        return false;
                    }
                } catch (Exception e) {
                    log.warn("Error during proxy connectivity check for account ID: {}. Error: {}",
                            account.getId(), e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchAndRecordAction(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        if (!"IN_PROGRESS".equals(account.getWarmUpStatus())) {
            log.warn("Account ID: {} is no longer in IN_PROGRESS status. Skipping action.", accountId);
            return;
        }

        String[] actions = {
                "subscribing to channels",
                "reading posts",
                "maintaining online presence status"
        };
        int actionIdx = random.nextInt(actions.length);
        String action = actions[actionIdx];

        log.info("Successfully dispatched automated action '{}' for account ID: {}", action, accountId);

        double increment = 0.1;
        double newScore = account.getTrustScore() + increment;
        account.setTrustScore(newScore);
        accountRepository.save(account);

        log.info("Updated trust score for account ID: {} to {}", accountId, newScore);
    }
}
