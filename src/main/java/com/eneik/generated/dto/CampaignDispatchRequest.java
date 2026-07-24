package com.eneik.generated.dto;

public class CampaignDispatchRequest {
    private Long telegramChatId;
    private String text;

    public CampaignDispatchRequest() {}

    public CampaignDispatchRequest(Long telegramChatId, String text) {
        this.telegramChatId = telegramChatId;
        this.text = text;
    }

    public Long getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
