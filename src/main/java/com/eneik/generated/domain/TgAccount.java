package com.eneik.generated.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;

@Entity
@Table(name = "tg_accounts")
public class TgAccount {
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_SPAM_BLOCK = "Temporary Spam-Block";
    public static final String STATUS_PERMANENT_BAN = "Permanent Ban";
    public static final String STATUS_REAUTH_REQUIRED = "Re-authorization Required";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "session_data", columnDefinition = "TEXT")
    private String sessionData;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "proxy_id")
    private Proxy proxy;

    @Column(name = "campaign_id", length = 36)
    private String campaignId;

    @Column(name = "daily_dispatch_limit", nullable = false)
    private Integer dailyDispatchLimit = 50;

    @Column(name = "daily_dispatch_count", nullable = false)
    private Integer dailyDispatchCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSessionData() { return sessionData; }
    public void setSessionData(String sessionData) { this.sessionData = sessionData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Proxy getProxy() { return proxy; }
    public void setProxy(Proxy proxy) { this.proxy = proxy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public Integer getDailyDispatchLimit() { return dailyDispatchLimit; }
    public void setDailyDispatchLimit(Integer dailyDispatchLimit) { this.dailyDispatchLimit = dailyDispatchLimit; }

    public Integer getDailyDispatchCount() { return dailyDispatchCount; }
    public void setDailyDispatchCount(Integer dailyDispatchCount) { this.dailyDispatchCount = dailyDispatchCount; }
}
