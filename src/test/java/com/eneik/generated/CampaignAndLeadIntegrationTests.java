package com.eneik.generated;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.repository.LeadRepository;
import com.eneik.generated.service.CampaignService;
import com.eneik.generated.service.LeadImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class CampaignAndLeadIntegrationTests {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private LeadImportService leadImportService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @BeforeEach
    public void setUp() {
        leadRepository.deleteAll();
        campaignRepository.deleteAll();
    }

    @Test
    public void testSaveCampaignConfigStoresSpintaxRules() {
        // Given a campaign config
        String campaignId = UUID.randomUUID().toString();
        String spintaxRules = "{Hello|Hi|Hey} there! Check out our {amazing|awesome} offer.";
        Campaign campaign = new Campaign(campaignId, "Promo Campaign #1", spintaxRules);

        // When saved
        Campaign savedCampaign = campaignService.saveCampaign(campaign);

        // Then spintax rules are stored in Postgres (H2 for test)
        assertNotNull(savedCampaign);
        assertEquals(campaignId, savedCampaign.getId());
        assertEquals(spintaxRules, savedCampaign.getSpintaxRules());

        // Also assert persistence
        Campaign fetched = campaignService.getCampaign(campaignId).orElse(null);
        assertNotNull(fetched);
        assertEquals(spintaxRules, fetched.getSpintaxRules());
    }

    @Test
    public void testLeadImportSavesContactsInLeadsTable() {
        // Given a campaign config
        String campaignId = UUID.randomUUID().toString();
        Campaign campaign = new Campaign(campaignId, "Outreach Campaign", "{Hello} {world}");
        campaignService.saveCampaign(campaign);

        // Given a lead import content
        String csvContent = """
                @john_doe,+1234567890,Interested in AI
                @jane_smith,,Need more info
                ,+987654321,No metadata
                """;

        // When processed
        List<Lead> imported = leadImportService.importLeads(campaignId, csvContent);

        // Then contacts are saved in the Leads table
        assertEquals(3, imported.size());

        List<Lead> storedLeads = leadRepository.findByCampaignId(campaignId);
        assertEquals(3, storedLeads.size());

        Lead first = storedLeads.stream().filter(l -> "@john_doe".equals(l.getUsername())).findFirst().orElse(null);
        assertNotNull(first);
        assertEquals("+1234567890", first.getPhoneNumber());
        assertEquals("Interested in AI", first.getMetadata());
        assertEquals("NEW", first.getStatus());

        Lead second = storedLeads.stream().filter(l -> "@jane_smith".equals(l.getUsername())).findFirst().orElse(null);
        assertNotNull(second);
        assertNull(second.getPhoneNumber());
        assertEquals("Need more info", second.getMetadata());

        Lead third = storedLeads.stream().filter(l -> l.getUsername() == null).findFirst().orElse(null);
        assertNotNull(third);
        assertEquals("+987654321", third.getPhoneNumber());
        assertEquals("No metadata", third.getMetadata());
    }
}
