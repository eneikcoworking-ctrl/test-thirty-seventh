package com.eneik.generated.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "warm_up_cycles")
public class WarmUpCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "status", nullable = false)
    private String status; // e.g., "ACTIVE", "COMPLETED", "FAILED"

    @Column(name = "target_score_increase", nullable = false)
    private double targetScoreIncrease;

    public WarmUpCycle() {}

    public WarmUpCycle(Account account, OffsetDateTime startedAt, String status, double targetScoreIncrease) {
        this.account = account;
        this.startedAt = startedAt;
        this.status = status;
        this.targetScoreIncrease = targetScoreIncrease;
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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTargetScoreIncrease() {
        return targetScoreIncrease;
    }

    public void setTargetScoreIncrease(double targetScoreIncrease) {
        this.targetScoreIncrease = targetScoreIncrease;
    }
}
