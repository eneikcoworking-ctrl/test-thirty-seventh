package com.eneik.generated.controller;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        leadRepository.deleteAll();
        campaignRepository.deleteAll();
    }

    @Test
    public void testSaveCampaign() throws Exception {
        Campaign campaign = new Campaign(UUID.randomUUID().toString(), "My Test Campaign", "{Hello|Hi}");

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaign)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(campaign.getId())))
                .andExpect(jsonPath("$.name", is("My Test Campaign")))
                .andExpect(jsonPath("$.spintaxRules", is("{Hello|Hi}")));
    }

    @Test
    public void testGetCampaignNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/campaigns/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetCampaignSuccess() throws Exception {
        String id = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(id, "Existing Campaign", "Rules");
        campaignRepository.save(campaign);

        mockMvc.perform(get("/api/v1/campaigns/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Existing Campaign")));
    }

    @Test
    public void testImportLeadsViaRawBody() throws Exception {
        String campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(campaignId, "Campaign with raw body import", "Rules");
        campaignRepository.save(campaign);

        String csvContent = "@alice,+1234,Metalover\n@bob,,Rockstar";

        mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/leads/import")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(csvContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("@alice")))
                .andExpect(jsonPath("$[0].phoneNumber", is("+1234")))
                .andExpect(jsonPath("$[1].username", is("@bob")));
    }

    @Test
    public void testImportLeadsViaMultipartFile() throws Exception {
        String campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(campaignId, "Campaign with multipart", "Rules");
        campaignRepository.save(campaign);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                MediaType.TEXT_PLAIN_VALUE,
                "@john_doe,+1234567890,Interested in AI\n@jane_smith,,Need more info".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/campaigns/" + campaignId + "/leads/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("@john_doe")))
                .andExpect(jsonPath("$[1].username", is("@jane_smith")));
    }

    @Test
    public void testImportLeadsCampaignNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/campaigns/non-existent-id/leads/import")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("@alice,+1234"))
                .andExpect(status().isNotFound());
    }
}
