package com.eneik.generated.dto;

public class TelegramAccountStatusRequest {
    private String phoneNumber;
    private String status;

    public TelegramAccountStatusRequest() {}

    public TelegramAccountStatusRequest(String phoneNumber, String status) {
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
