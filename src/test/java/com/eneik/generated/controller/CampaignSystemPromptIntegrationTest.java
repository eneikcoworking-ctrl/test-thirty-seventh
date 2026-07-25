package com.eneik.generated.controller;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
