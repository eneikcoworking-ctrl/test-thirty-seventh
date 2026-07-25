package com.eneik.generated.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "spintax_rules", columnDefinition = "TEXT")
    private String spintaxRules;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "ai_persona", columnDefinition = "TEXT")
    private String aiPersona;

    @Column(name = "sales_goals", columnDefinition = "TEXT")
    private String salesGoals;

    @Column(name = "tone_of_voice", columnDefinition = "TEXT")
    private String toneOfVoice;

    @Column(name = "product_faqs", columnDefinition = "TEXT")
    private String productFaqs;

    @Column(name = "qualification_rules", columnDefinition = "TEXT")
    private String qualificationRules;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Campaign() {
    }

    public Campaign(String id, String name, String spintaxRules) {
        this.id = id;
        this.name = name;
        this.spintaxRules = spintaxRules;
        this.createdAt = LocalDateTime.now();
    }

    public Campaign(String id, String name, String spintaxRules, String systemPrompt, String aiPersona,
                    String salesGoals, String toneOfVoice, String productFaqs, String qualificationRules) {
        this.id = id;
        this.name = name;
        this.spintaxRules = spintaxRules;
        this.systemPrompt = systemPrompt;
        this.aiPersona = aiPersona;
        this.salesGoals = salesGoals;
        this.toneOfVoice = toneOfVoice;
        this.productFaqs = productFaqs;
        this.qualificationRules = qualificationRules;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpintaxRules() {
        return spintaxRules;
    }

    public void setSpintaxRules(String spintaxRules) {
        this.spintaxRules = spintaxRules;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
