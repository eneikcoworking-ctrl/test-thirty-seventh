package com.eneik.generated.dto;

import java.util.UUID;

public class CampaignAssignmentResponse {
    private UUID campaignId;
    private Long accountId;
    private String accountType;
    private String status;

    public CampaignAssignmentResponse() {}

    public CampaignAssignmentResponse(UUID campaignId, Long accountId, String accountType, String status) {
        this.campaignId = campaignId;
        this.accountId = accountId;
        this.accountType = accountType;
        this.status = status;
    }

    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
