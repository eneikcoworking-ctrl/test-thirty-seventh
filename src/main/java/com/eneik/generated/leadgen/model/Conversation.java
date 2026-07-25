package com.eneik.generated.leadgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversations")
public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private Long telegramChatId;
    private String leadName;
    private String leadUsername;
    private String leadPhone;
    private String status; // ACTIVE, ESCALATED, RESOLVED, PAUSED
    private String assignedAgentId;
    private OffsetDateTime lastMessageAt;
    private OffsetDateTime createdAt;

    public Conversation() {}

    public Conversation(String id, Long telegramChatId, String leadName, String leadUsername, String leadPhone, String status, String assignedAgentId, OffsetDateTime lastMessageAt, OffsetDateTime createdAt) {
        this.id = id;
        this.telegramChatId = telegramChatId;
        this.leadName = leadName;
        this.leadUsername = leadUsername;
        this.leadPhone = leadPhone;
        this.status = status;
        this.assignedAgentId = assignedAgentId;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public String getLeadUsername() {
        return leadUsername;
    }

    public void setLeadUsername(String leadUsername) {
        this.leadUsername = leadUsername;
    }

    public String getLeadPhone() {
        return leadPhone;
    }

    public void setLeadPhone(String leadPhone) {
        this.leadPhone = leadPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public OffsetDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
