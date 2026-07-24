package com.eneik.generated.leadgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telegramAccountId;
    private String leadId;
    private String leadUsername;
    private String lastMessage;
    private OffsetDateTime lastMessageTimestamp;

    public Conversation() {}

    public Conversation(String telegramAccountId, String leadId, String leadUsername, String lastMessage, OffsetDateTime lastMessageTimestamp) {
        this.telegramAccountId = telegramAccountId;
        this.leadId = leadId;
        this.leadUsername = leadUsername;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTelegramAccountId() {
        return telegramAccountId;
    }

    public void setTelegramAccountId(String telegramAccountId) {
        this.telegramAccountId = telegramAccountId;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getLeadUsername() {
        return leadUsername;
    }

    public void setLeadUsername(String leadUsername) {
        this.leadUsername = leadUsername;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public OffsetDateTime getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(OffsetDateTime lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
