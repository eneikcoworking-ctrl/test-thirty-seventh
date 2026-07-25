package com.eneik.generated.controller;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignSystemPromptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

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
}
