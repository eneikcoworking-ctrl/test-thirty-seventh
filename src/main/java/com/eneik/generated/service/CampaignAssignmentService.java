package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.model.Account;
import com.eneik.generated.repository.AccountRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.dto.CampaignAssignmentRequest.AccountType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class CampaignAssignmentService {

    private final TgAccountRepository tgAccountRepository;
    private final AccountRepository accountRepository;

    public CampaignAssignmentService(TgAccountRepository tgAccountRepository, AccountRepository accountRepository) {
        this.tgAccountRepository = tgAccountRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Validates and assigns a campaign to an account.
     * Rejects accounts under 1 month old.
     *
     * @param campaignId the UUID of the campaign
     * @param accountId the database ID of the account
     * @param accountType the type of the account (TG_ACCOUNT or WARM_UP_ACCOUNT)
     * @return true if assignment was successful
     */
    public boolean assignCampaign(UUID campaignId, Long accountId, AccountType accountType) {
        if (campaignId == null || accountId == null || accountType == null) {
            throw new IllegalArgumentException("Campaign ID, Account ID, and Account Type must not be null");
        }

        if (accountType == AccountType.TG_ACCOUNT) {
            TgAccount tgAccount = tgAccountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("TgAccount not found with ID: " + accountId));

            LocalDateTime createdAt = tgAccount.getCreatedAt();
            if (createdAt == null) {
                throw new IllegalArgumentException("TgAccount creation timestamp is null");
            }

            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            if (createdAt.isAfter(oneMonthAgo)) {
                throw new IllegalArgumentException("Campaign assignment rejected: Account is under 1 month old");
            }

        } else if (accountType == AccountType.WARM_UP_ACCOUNT) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

            OffsetDateTime createdAt = account.getCreatedAt();
            if (createdAt == null) {
                throw new IllegalArgumentException("Account creation timestamp is null");
            }

            OffsetDateTime oneMonthAgo = OffsetDateTime.now().minusMonths(1);
            if (createdAt.isAfter(oneMonthAgo)) {
                throw new IllegalArgumentException("Campaign assignment rejected: Account is under 1 month old");
            }
        } else {
            throw new IllegalArgumentException("Unsupported account type: " + accountType);
        }

        // Return true on successful validation
        return true;
    }
}
