package com.eneik.generated.leadgen.controller;

import java.time.OffsetDateTime;

public class ConversationDto {
    private Long id;
    private String telegramAccountId;
    private String leadId;
    private String leadUsername;
    private String lastMessage;
    private OffsetDateTime lastMessageTimestamp;

    public ConversationDto() {}

    public ConversationDto(Long id, String telegramAccountId, String leadId, String leadUsername, String lastMessage, OffsetDateTime lastMessageTimestamp) {
        this.id = id;
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
