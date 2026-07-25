package com.eneik.generated.dto;

public class CreateTaskRequest {
    private String id;
    private String sessionId;

    public CreateTaskRequest() {
    }

    public CreateTaskRequest(String id, String sessionId) {
        this.id = id;
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
