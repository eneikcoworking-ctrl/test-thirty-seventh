package com.eneik.generated.dto;

import java.util.UUID;

public class CampaignAssignmentRequest {
    private UUID campaignId;
    private Long accountId;
    private AccountType accountType;

    public enum AccountType {
        TG_ACCOUNT,
        WARM_UP_ACCOUNT
    }

    public CampaignAssignmentRequest() {}

    public CampaignAssignmentRequest(UUID campaignId, Long accountId, AccountType accountType) {
        this.campaignId = campaignId;
        this.accountId = accountId;
        this.accountType = accountType;
    }

    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
}
