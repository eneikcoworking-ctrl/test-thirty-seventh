package com.eneik.generated.leadgen.controller;

import java.time.OffsetDateTime;

public class MessageDto {
    private String id;
    private String conversationId;
    private String text;
    private String senderType; // LEAD, AI_AGENT, HUMAN_REPRESENTATIVE
    private OffsetDateTime sentAt;
    private String senderName;

    public MessageDto() {}

    public MessageDto(String id, String conversationId, String text, String senderType, OffsetDateTime sentAt, String senderName) {
        this.id = id;
        this.conversationId = conversationId;
        this.text = text;
        this.senderType = senderType;
        this.sentAt = sentAt;
        this.senderName = senderName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
