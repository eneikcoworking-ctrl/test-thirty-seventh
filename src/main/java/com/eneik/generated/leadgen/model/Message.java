package com.eneik.generated.leadgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telegramAccountId;
    private String leadId;
    private String content;
    private String direction; // INBOUND, OUTBOUND
    private String status; // PENDING, SENT, FAILED
    private OffsetDateTime timestamp;

    public Message() {}

    public Message(String telegramAccountId, String leadId, String content, String direction, String status, OffsetDateTime timestamp) {
        this.telegramAccountId = telegramAccountId;
        this.leadId = leadId;
        this.content = content;
        this.direction = direction;
        this.status = status;
        this.timestamp = timestamp;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
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
