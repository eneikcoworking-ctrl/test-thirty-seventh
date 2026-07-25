package com.eneik.generated.domain;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    UNDER_REVIEW,
    REVIEW_REJECTED,
    COMPLETED,
    TERMINAL;

    public boolean isTerminal() {
        return this == TERMINAL || this == COMPLETED;
    }
}
