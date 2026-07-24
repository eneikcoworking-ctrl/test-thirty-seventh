package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", unique = true, nullable = false)
    private String telegramId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "warm_up_status", nullable = false)
    private String warmUpStatus; // e.g., "NOT_STARTED", "IN_PROGRESS", "COMPLETED"

    @Column(name = "trust_score", nullable = false)
    private double trustScore = 0.0;

    public Account() {}

    public Account(String telegramId, String phoneNumber, OffsetDateTime createdAt, String warmUpStatus, double trustScore) {
        this.telegramId = telegramId;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.warmUpStatus = warmUpStatus;
        this.trustScore = trustScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getWarmUpStatus() {
        return warmUpStatus;
    }

    public void setWarmUpStatus(String warmUpStatus) {
        this.warmUpStatus = warmUpStatus;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }
}
