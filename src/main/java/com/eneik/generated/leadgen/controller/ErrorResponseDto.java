package com.eneik.generated.leadgen.controller;

import java.time.OffsetDateTime;
import java.util.List;

public class ErrorResponseDto {
    private String errorCode;
    private String message;
    private OffsetDateTime timestamp;
    private List<String> details;

    public ErrorResponseDto() {}

    public ErrorResponseDto(String errorCode, String message, OffsetDateTime timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ErrorResponseDto(String errorCode, String message, OffsetDateTime timestamp, List<String> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
