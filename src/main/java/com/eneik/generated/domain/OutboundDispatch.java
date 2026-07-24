package com.eneik.generated.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_dispatches")
public class OutboundDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tg_account_id", nullable = false)
    private TgAccount tgAccount;

    @Column(name = "campaign_id", nullable = false, length = 36)
    private String campaignId;

    @Column(name = "recipient_phone_or_username", nullable = false)
    private String recipientPhoneOrUsername;

    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    public OutboundDispatch() {
    }

    public OutboundDispatch(TgAccount tgAccount, String campaignId, String recipientPhoneOrUsername) {
        this.tgAccount = tgAccount;
        this.campaignId = campaignId;
        this.recipientPhoneOrUsername = recipientPhoneOrUsername;
        this.dispatchedAt = LocalDateTime.now();
    }

    public OutboundDispatch(TgAccount tgAccount, String campaignId, String recipientPhoneOrUsername, LocalDateTime dispatchedAt) {
        this.tgAccount = tgAccount;
        this.campaignId = campaignId;
        this.recipientPhoneOrUsername = recipientPhoneOrUsername;
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

    public String getRecipientPhoneOrUsername() {
        return recipientPhoneOrUsername;
    }

    public void setRecipientPhoneOrUsername(String recipientPhoneOrUsername) {
        this.recipientPhoneOrUsername = recipientPhoneOrUsername;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}
