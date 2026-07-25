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

    /**
     * Overloaded method that also supplies the system prompts (defining AI persona, sales goal, tone, FAQs, qualification rules)
     * to the AI generation/rephrasing engine.
     */
    public String personalize(String template, String metadata, String systemPrompt, String aiPersona,
                              String salesGoals, String toneOfVoice, String productFaqs, String qualificationRules) {
        if (template == null) {
            return null;
        }

        StringBuilder promptBuilder = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            promptBuilder.append("System Prompt: ").append(systemPrompt.trim()).append("; ");
        }
        if (aiPersona != null && !aiPersona.trim().isEmpty()) {
            promptBuilder.append("Persona: ").append(aiPersona.trim()).append("; ");
        }
        if (salesGoals != null && !salesGoals.trim().isEmpty()) {
            promptBuilder.append("Goals: ").append(salesGoals.trim()).append("; ");
        }
        if (toneOfVoice != null && !toneOfVoice.trim().isEmpty()) {
            promptBuilder.append("Tone: ").append(toneOfVoice.trim()).append("; ");
        }
        if (productFaqs != null && !productFaqs.trim().isEmpty()) {
            promptBuilder.append("FAQs: ").append(productFaqs.trim()).append("; ");
        }
        if (qualificationRules != null && !qualificationRules.trim().isEmpty()) {
            promptBuilder.append("Rules: ").append(qualificationRules.trim()).append("; ");
        }

        String promptContext = promptBuilder.toString().trim();
        if (!promptContext.isEmpty()) {
            // Trim trailing semicolon if present
            if (promptContext.endsWith(";")) {
                promptContext = promptContext.substring(0, promptContext.length() - 1);
            }
            String enrichedMetadata = (metadata != null && !metadata.trim().isEmpty())
                    ? metadata.trim() + " | Context: " + promptContext
                    : "Context: " + promptContext;
            return rephraser.apply(template, enrichedMetadata);
        }

        return personalize(template, metadata);
    }
}
