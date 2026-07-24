package com.eneik.generated.leadgen.controller;

public class ManualMessageRequestDto {
    private String telegramAccountId;
    private String leadId;
    private String message;

    public ManualMessageRequestDto() {}

    public ManualMessageRequestDto(String telegramAccountId, String leadId, String message) {
        this.telegramAccountId = telegramAccountId;
        this.leadId = leadId;
        this.message = message;
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
}
