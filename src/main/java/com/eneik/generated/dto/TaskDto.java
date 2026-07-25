package com.eneik.generated.dto;

public class TaskDto {
    private String id;
    private String status;
    private int rejectionCount;
    private String sessionId;

    public TaskDto() {
    }

    public TaskDto(String id, String status, int rejectionCount, String sessionId) {
        this.id = id;
        this.status = status;
        this.rejectionCount = rejectionCount;
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRejectionCount() {
        return rejectionCount;
    }

    public void setRejectionCount(int rejectionCount) {
        this.rejectionCount = rejectionCount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
