package com.eneik.generated.controller;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignSystemPromptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        campaignRepository.deleteAll();
    }

    @Test
    public void testCampaignExposedWithCustomSystemPrompt() throws Exception {
        String campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(
                campaignId,
                "Enterprise AI Promo",
                "{Hello|Hi}",
                "Be a highly helpful enterprise consultant.",
                "Executive Assistant Persona",
                "Acquire 5 corporate demo signups",
                "Highly professional and articulate",
                "Q: Pricing? A: Custom tiers.",
                "Company must have over 50 employees"
        );
        campaignRepository.save(campaign);

        mockMvc.perform(get("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(campaignId)))
                .andExpect(jsonPath("$[0].name", is("Enterprise AI Promo")))
                .andExpect(jsonPath("$[0].spintaxRules", is("{Hello|Hi}")))
                .andExpect(jsonPath("$[0].systemPrompt", is("Be a highly helpful enterprise consultant.")))
                .andExpect(jsonPath("$[0].aiPersona", is("Executive Assistant Persona")))
                .andExpect(jsonPath("$[0].salesGoals", is("Acquire 5 corporate demo signups")))
                .andExpect(jsonPath("$[0].toneOfVoice", is("Highly professional and articulate")))
                .andExpect(jsonPath("$[0].productFaqs", is("Q: Pricing? A: Custom tiers.")))
                .andExpect(jsonPath("$[0].qualificationRules", is("Company must have over 50 employees")));
    }

    @Test
    public void testCampaignExposedWithEmptyOrNullPrompts() throws Exception {
        String campaignId = UUID.randomUUID().toString();
        // Null values
        Campaign campaignNull = new Campaign(campaignId, "Null Promo", "{Hello}");
        campaignRepository.save(campaignNull);

        String campaignIdEmpty = UUID.randomUUID().toString();
        // Empty string values
        Campaign campaignEmpty = new Campaign(
                campaignIdEmpty,
                "Empty Promo",
                "{Hi}",
                "",
                "",
                "",
                "",
                "",
                ""
        );
        campaignRepository.save(campaignEmpty);

        mockMvc.perform(get("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Verify null campaign
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].systemPrompt", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].aiPersona", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].salesGoals", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].toneOfVoice", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].productFaqs", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.id=='" + campaignId + "')].qualificationRules", contains((Object) null)))
                // Verify empty campaign
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].systemPrompt", contains("")))
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].aiPersona", contains("")))
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].salesGoals", contains("")))
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].toneOfVoice", contains("")))
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].productFaqs", contains("")))
                .andExpect(jsonPath("$[?(@.id=='" + campaignIdEmpty + "')].qualificationRules", contains("")));
    }

    @Test
    public void testUpdateSystemPrompt_Success() throws Exception {
        // Given a newly created campaign
        String id = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(id, "Test Campaign", "rules");
        campaignRepository.save(campaign);

        // When the administrator updates the system prompt settings
        Map<String, String> payload = new HashMap<>();
        payload.put("systemPrompt", "You are a helpful AI assistant.");
        payload.put("aiPersona", "Friendly sales agent");
        payload.put("salesGoals", "Onboard new prospects");
        payload.put("toneOfVoice", "Empathetic");
        payload.put("productFaqs", "Q: What is Eneik? A: Amazing tech.");
        payload.put("qualificationRules", "Must have a Telegram account.");

        // Then the custom prompt is saved successfully
        mockMvc.perform(put("/api/v1/campaigns/{id}/system-prompt", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt", is("You are a helpful AI assistant.")))
                .andExpect(jsonPath("$.aiPersona", is("Friendly sales agent")))
                .andExpect(jsonPath("$.salesGoals", is("Onboard new prospects")))
                .andExpect(jsonPath("$.toneOfVoice", is("Empathetic")))
                .andExpect(jsonPath("$.productFaqs", is("Q: What is Eneik? A: Amazing tech.")))
                .andExpect(jsonPath("$.qualificationRules", is("Must have a Telegram account.")));

        // Verify database state
        Campaign updatedCampaign = campaignRepository.findById(id).orElseThrow();
        assertThat(updatedCampaign.getSystemPrompt()).isEqualTo("You are a helpful AI assistant.");
        assertThat(updatedCampaign.getAiPersona()).isEqualTo("Friendly sales agent");
        assertThat(updatedCampaign.getSalesGoals()).isEqualTo("Onboard new prospects");
        assertThat(updatedCampaign.getToneOfVoice()).isEqualTo("Empathetic");
        assertThat(updatedCampaign.getProductFaqs()).isEqualTo("Q: What is Eneik? A: Amazing tech.");
        assertThat(updatedCampaign.getQualificationRules()).isEqualTo("Must have a Telegram account.");
    }

    @Test
    public void testUpdateSystemPrompt_WithInvalidData_RejectsAndReturns400() throws Exception {
        // Given a campaign
        String id = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(id, "Test Campaign", "rules");
        campaignRepository.save(campaign);

        // When saving invalid prompt data is attempted (e.g. systemPrompt contains invalid_prompt_test_fail trigger or blank fields)
        Map<String, String> payload = new HashMap<>();
        payload.put("systemPrompt", "  "); // blank string
        payload.put("aiPersona", "Friendly sales agent");

        // Then the system rejects the update and logs an error, returning 400 Bad Request
        mockMvc.perform(put("/api/v1/campaigns/{id}/system-prompt", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_PROMPT_DATA")))
                .andExpect(jsonPath("$.message", is("System prompt must not be empty or blank")));

        // Test with disallowed keyword
        Map<String, String> invalidKeywordPayload = new HashMap<>();
        invalidKeywordPayload.put("systemPrompt", "This is invalid_prompt_test_fail prompt.");

        mockMvc.perform(put("/api/v1/campaigns/{id}/system-prompt", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidKeywordPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_PROMPT_DATA")))
                .andExpect(jsonPath("$.message", is("System prompt contains disallowed invalid keywords")));
    }
}
