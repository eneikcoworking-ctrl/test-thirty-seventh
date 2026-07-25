package com.eneik.generated.dto;

public class TelegramInboundMessageRequest {
    private Long telegramChatId;
    private String text;
    private String mediaType;

    public TelegramInboundMessageRequest() {}

    public TelegramInboundMessageRequest(Long telegramChatId, String text, String mediaType) {
        this.telegramChatId = telegramChatId;
        this.text = text;
        this.mediaType = mediaType;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
