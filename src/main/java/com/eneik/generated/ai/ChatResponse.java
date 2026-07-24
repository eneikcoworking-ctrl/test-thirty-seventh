package com.eneik.generated.ai;

import java.util.List;

public class ChatResponse {
    private final List<Generation> generations;

    public ChatResponse(List<Generation> generations) {
        this.generations = generations;
    }

    public List<Generation> getGenerations() {
        return generations;
    }

    public Generation getResult() {
        if (generations != null && !generations.isEmpty()) {
            return generations.get(0);
        }
        return null;
    }
}
