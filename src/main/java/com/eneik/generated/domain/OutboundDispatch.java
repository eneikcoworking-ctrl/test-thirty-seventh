package com.eneik.generated.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import java.time.OffsetDateTime;

@Entity
@Table(name = "outbound_dispatches")
public class OutboundDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tg_account_id", nullable = false)
    private TgAccount tgAccount;

    @Column(name = "campaign_id")
    private String campaignId;

    @Column(name = "dispatched_at", nullable = false)
    private OffsetDateTime dispatchedAt = OffsetDateTime.now();

    public OutboundDispatch() {}

    public OutboundDispatch(TgAccount tgAccount, String campaignId, OffsetDateTime dispatchedAt) {
        this.tgAccount = tgAccount;
        this.campaignId = campaignId;
        this.dispatchedAt = dispatchedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TgAccount getTgAccount() {
        return tgAccount;
    }

    public void setTgAccount(TgAccount tgAccount) {
        this.tgAccount = tgAccount;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public OffsetDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(OffsetDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}
