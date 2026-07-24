package com.eneik.generated.leadgen.controller;

import java.time.OffsetDateTime;

public class ErrorResponseDto {
    private String errorCode;
    private String errorMessage;
    private OffsetDateTime timestamp;

    public ErrorResponseDto() {}

    public ErrorResponseDto(String errorCode, String errorMessage, OffsetDateTime timestamp) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
