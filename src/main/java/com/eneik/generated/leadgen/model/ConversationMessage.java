package com.eneik.generated.leadgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage implements java.io.Serializable {
    private static final long serialVersionUID = 6677889900L;

    @Id
    private String id;

    private String conversationId;
    private String text;
    private String senderType; // LEAD, AI_AGENT, HUMAN_REPRESENTATIVE
    private OffsetDateTime sentAt;
    private String senderName;

    public ConversationMessage() {}

    public ConversationMessage(String id, String conversationId, String text, String senderType, OffsetDateTime sentAt, String senderName) {
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
