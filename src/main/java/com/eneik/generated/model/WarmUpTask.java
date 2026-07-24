package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "warm_up_tasks")
public class WarmUpTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "channel_username", nullable = false)
    private String channelUsername;

    @Column(name = "status", nullable = false)
    private String status; // e.g. "PENDING", "COMPLETED", "FAILED"

    @Column(name = "subscribed", nullable = false)
    private boolean subscribed = false;

    @Column(name = "marked_as_read", nullable = false)
    private boolean markedAsRead = false;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "applied_delay_ms")
    private Long appliedDelayMs;

    public WarmUpTask() {}

    public WarmUpTask(Account account, String channelUsername, String status) {
        this.account = account;
        this.channelUsername = channelUsername;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getChannelUsername() {
        return channelUsername;
    }

    public void setChannelUsername(String channelUsername) {
        this.channelUsername = channelUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean isMarkedAsRead() {
        return markedAsRead;
    }

    public void setMarkedAsRead(boolean markedAsRead) {
        this.markedAsRead = markedAsRead;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Long getAppliedDelayMs() {
        return appliedDelayMs;
    }

    public void setAppliedDelayMs(Long appliedDelayMs) {
        this.appliedDelayMs = appliedDelayMs;
    }
}
