package com.eneik.generated.dto;

public class DispatchRequest {
    private Long tgAccountId;
    private String leadIdentifier;
    private String message;

    public Long getTgAccountId() {
        return tgAccountId;
    }

    public void setTgAccountId(Long tgAccountId) {
        this.tgAccountId = tgAccountId;
    }

    public String getLeadIdentifier() {
        return leadIdentifier;
    }

    public void setLeadIdentifier(String leadIdentifier) {
        this.leadIdentifier = leadIdentifier;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
