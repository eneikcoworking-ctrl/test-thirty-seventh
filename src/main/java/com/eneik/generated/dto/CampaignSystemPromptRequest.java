package com.eneik.generated.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class CampaignSystemPromptRequest {
    public static final int MAX_PROMPT_LENGTH = 4000;

    @Size(max = MAX_PROMPT_LENGTH, message = "System prompt exceeds maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String systemPrompt;

    @Size(max = MAX_PROMPT_LENGTH, message = "AI Persona exceeds maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String aiPersona;

    @Size(max = MAX_PROMPT_LENGTH, message = "Sales goals exceed maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String salesGoals;

    @Size(max = MAX_PROMPT_LENGTH, message = "Tone of voice exceeds maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String toneOfVoice;

    @Size(max = MAX_PROMPT_LENGTH, message = "Product FAQs exceed maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String productFaqs;

    @Size(max = MAX_PROMPT_LENGTH, message = "Qualification rules exceed maximum length of 4000 characters")
    @Pattern(regexp = "(?s)^$|^(?!\\s+$).*$", message = "Must not be whitespace only")
    private String qualificationRules;

    public CampaignSystemPromptRequest() {
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getAiPersona() {
        return aiPersona;
    }

    public void setAiPersona(String aiPersona) {
        this.aiPersona = aiPersona;
    }

    public String getSalesGoals() {
        return salesGoals;
    }

    public void setSalesGoals(String salesGoals) {
        this.salesGoals = salesGoals;
    }

    public String getToneOfVoice() {
        return toneOfVoice;
    }

    public void setToneOfVoice(String toneOfVoice) {
        this.toneOfVoice = toneOfVoice;
    }

    public String getProductFaqs() {
        return productFaqs;
    }

    public void setProductFaqs(String productFaqs) {
        this.productFaqs = productFaqs;
    }

    public String getQualificationRules() {
        return qualificationRules;
    }

    public void setQualificationRules(String qualificationRules) {
        this.qualificationRules = qualificationRules;
    }
}
