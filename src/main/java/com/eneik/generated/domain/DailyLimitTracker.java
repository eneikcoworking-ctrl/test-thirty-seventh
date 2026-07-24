package com.eneik.generated.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_limit_trackers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tg_account_id", "tracked_date"})
})
public class DailyLimitTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tg_account_id", nullable = false)
    private TgAccount tgAccount;

    @Column(name = "tracked_date", nullable = false)
    private LocalDate trackedDate;

    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

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

    public LocalDate getTrackedDate() {
        return trackedDate;
    }

    public void setTrackedDate(LocalDate trackedDate) {
        this.trackedDate = trackedDate;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }
}
