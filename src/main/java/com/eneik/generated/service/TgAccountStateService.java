package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TgAccountStateService {

    private final TgAccountRepository tgAccountRepository;

    public TgAccountStateService(TgAccountRepository tgAccountRepository) {
        this.tgAccountRepository = tgAccountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementDispatchCount(Long accountId) {
        TgAccount account = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.setDailyDispatchCount(account.getDailyDispatchCount() + 1);
        tgAccountRepository.save(account);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFloodWait(Long accountId) {
        TgAccount account = tgAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.setStatus("FLOOD_WAIT");
        tgAccountRepository.save(account);
    }
}
