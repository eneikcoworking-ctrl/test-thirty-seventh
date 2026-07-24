package com.eneik.generated.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "campaign_id", nullable = false, length = 36)
    private String campaignId;

    @Column(length = 255)
    private String username;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(nullable = false, length = 50)
    private String status = "NEW";

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Lead() {
    }

    public Lead(String id, String campaignId, String username, String phoneNumber, String status, String metadata) {
        this.id = id;
        this.campaignId = campaignId;
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? status : "NEW";
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
