package com.eneik.generated.leadgen.controller;

import java.time.OffsetDateTime;

public class MessageResponseDto {
    private String messageId;
    private String telegramAccountId;
    private String leadId;
    private String message;
    private String status;
    private OffsetDateTime timestamp;

    public MessageResponseDto() {}

    public MessageResponseDto(String messageId, String telegramAccountId, String leadId, String message, String status, OffsetDateTime timestamp) {
        this.messageId = messageId;
        this.telegramAccountId = telegramAccountId;
        this.leadId = leadId;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
