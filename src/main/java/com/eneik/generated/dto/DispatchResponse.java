package com.eneik.generated.dto;

public class DispatchResponse {
    private String status;
    private String message;

    public DispatchResponse() {}

    public DispatchResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
