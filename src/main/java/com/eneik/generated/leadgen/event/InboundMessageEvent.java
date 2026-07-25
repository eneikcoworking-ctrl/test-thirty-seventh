package com.eneik.generated.leadgen.event;

public class InboundMessageEvent {
    private final Long telegramChatId;
    private final String leadName;
    private final String text;
    private final String leadUsername;
    private final String leadPhone;

    public InboundMessageEvent(Long telegramChatId, String leadName, String text, String leadUsername, String leadPhone) {
        this.telegramChatId = telegramChatId;
        this.leadName = leadName;
        this.text = text;
        this.leadUsername = leadUsername;
        this.leadPhone = leadPhone;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getLeadName() {
        return leadName;
    }

    public String getText() {
        return text;
    }

    public String getLeadUsername() {
        return leadUsername;
    }

    public String getLeadPhone() {
        return leadPhone;
    }
}
