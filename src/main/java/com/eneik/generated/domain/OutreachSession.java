package com.eneik.generated.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outreach_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tg_account_id", "lead_identifier"})
})
public class OutreachSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tg_account_id", nullable = false)
    private TgAccount tgAccount;

    @Column(name = "lead_identifier", nullable = false)
    private String leadIdentifier;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getLeadIdentifier() {
        return leadIdentifier;
    }

    public void setLeadIdentifier(String leadIdentifier) {
        this.leadIdentifier = leadIdentifier;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Boolean getBlocked() {
        return isBlocked;
    }

    public void setBlocked(Boolean blocked) {
        isBlocked = blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
