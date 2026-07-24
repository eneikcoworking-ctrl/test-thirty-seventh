package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.function.BiFunction;

@Service
public class LlmPersonalizationService {

    // Default rephrasing strategy simulating LLM response based on template and metadata
    private BiFunction<String, String, String> rephraser = (template, metadata) -> {
        if (metadata == null || metadata.trim().isEmpty()) {
            return template;
        }
        return "Personalized offer based on [" + metadata.trim() + "]: " + template;
    };

    /**
     * Sets a custom rephrasing behavior, e.g. for mock LLM testing.
     */
    public void setRephraser(BiFunction<String, String, String> rephraser) {
        if (rephraser != null) {
            this.rephraser = rephraser;
        }
    }

    /**
     * Rephrases/personalizes the outreach offer based on the provided lead metadata.
     */
    public String personalize(String template, String metadata) {
        if (template == null) {
            return null;
        }
        return rephraser.apply(template, metadata);
    }
}
