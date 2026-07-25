package com.eneik.generated.service;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonalizationEngine {

    private final SpintaxService spintaxService;
    private final LlmPersonalizationService llmPersonalizationService;

    @Autowired
    public PersonalizationEngine(SpintaxService spintaxService, LlmPersonalizationService llmPersonalizationService) {
        this.spintaxService = spintaxService;
        this.llmPersonalizationService = llmPersonalizationService;
    }

    /**
     * Generates a personalized outreach message for a given lead and campaign.
     *
     * Process:
     * 1. Retrieves the campaign's spintax rules template.
     * 2. Rephrases/personalizes the template using Lead metadata & campaign custom prompt templates (via LLM rephraser).
     * 3. Evaluates spintax on the personalized template to resolve options randomly.
     */
    public String generatePersonalizedMessage(Campaign campaign, Lead lead) {
        if (campaign == null || lead == null) {
            return null;
        }
        String template = campaign.getSpintaxRules();
        if (template == null || template.isEmpty()) {
            return "";
        }

        // 1. Personalize template via LLM based on lead metadata and campaign custom prompt context
        String personalizedTemplate = llmPersonalizationService.personalize(
                template,
                lead.getMetadata(),
                campaign.getSystemPrompt(),
                campaign.getAiPersona(),
                campaign.getSalesGoals(),
                campaign.getToneOfVoice(),
                campaign.getProductFaqs(),
                campaign.getQualificationRules()
        );

        // 2. Evaluate spintax to generate the final randomized variant
        return spintaxService.evaluate(personalizedTemplate);
    }
}
