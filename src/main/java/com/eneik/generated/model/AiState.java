package com.eneik.generated.model;

public enum AiState {
    ACTIVE("Active"),
    STOPPED("Stopped"),
    PAUSED("Paused"),
    AWAITING_HUMAN_INTERVENTION("Awaiting Human Intervention");

    private final String displayName;

    AiState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
