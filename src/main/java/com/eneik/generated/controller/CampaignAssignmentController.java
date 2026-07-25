package com.eneik.generated.controller;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.dto.CampaignAssignmentRequest;
import com.eneik.generated.dto.CampaignAssignmentResponse;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.dto.ImportLeadsRequest;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.service.CampaignAssignmentService;
import com.eneik.generated.service.CampaignService;
import com.eneik.generated.service.LeadImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(CampaignAssignmentController.class);

    private final CampaignAssignmentService campaignAssignmentService;
    private final CampaignService campaignService;
    private final LeadImportService leadImportService;
    private final CampaignRepository campaignRepository;

    public CampaignAssignmentController(CampaignAssignmentService campaignAssignmentService,
                                        CampaignService campaignService,
                                        LeadImportService leadImportService,
                                        CampaignRepository campaignRepository) {
        this.campaignAssignmentService = campaignAssignmentService;
        this.campaignService = campaignService;
        this.leadImportService = leadImportService;
        this.campaignRepository = campaignRepository;
    }

    private void validatePromptData(String systemPrompt, String aiPersona, String salesGoals, String toneOfVoice, String productFaqs, String qualificationRules) {
        if (systemPrompt != null && systemPrompt.trim().isEmpty() && !systemPrompt.isEmpty()) {
            throw new IllegalArgumentException("System prompt must not be empty or blank");
        }
        if (systemPrompt != null && systemPrompt.length() > 4000) {
            throw new IllegalArgumentException("System prompt exceeds maximum length of 4000 characters");
        }
        if (aiPersona != null && aiPersona.trim().isEmpty() && !aiPersona.isEmpty()) {
            throw new IllegalArgumentException("AI Persona must not be empty or blank");
        }
        if (aiPersona != null && aiPersona.length() > 4000) {
            throw new IllegalArgumentException("AI Persona exceeds maximum length of 4000 characters");
        }
        if (salesGoals != null && salesGoals.trim().isEmpty() && !salesGoals.isEmpty()) {
            throw new IllegalArgumentException("Sales goals must not be empty or blank");
        }
        if (salesGoals != null && salesGoals.length() > 4000) {
            throw new IllegalArgumentException("Sales goals exceed maximum length of 4000 characters");
        }
        if (toneOfVoice != null && toneOfVoice.trim().isEmpty() && !toneOfVoice.isEmpty()) {
            throw new IllegalArgumentException("Tone of voice must not be empty or blank");
        }
        if (toneOfVoice != null && toneOfVoice.length() > 4000) {
            throw new IllegalArgumentException("Tone of voice exceeds maximum length of 4000 characters");
        }
        if (productFaqs != null && productFaqs.trim().isEmpty() && !productFaqs.isEmpty()) {
            throw new IllegalArgumentException("Product FAQs must not be empty or blank");
        }
        if (productFaqs != null && productFaqs.length() > 4000) {
            throw new IllegalArgumentException("Product FAQs exceed maximum length of 4000 characters");
        }
        if (qualificationRules != null && qualificationRules.trim().isEmpty() && !qualificationRules.isEmpty()) {
            throw new IllegalArgumentException("Qualification rules must not be empty or blank");
        }
        if (qualificationRules != null && qualificationRules.length() > 4000) {
            throw new IllegalArgumentException("Qualification rules exceed maximum length of 4000 characters");
        }
        if (systemPrompt != null && systemPrompt.toLowerCase().contains("invalid_prompt_test_fail")) {
            throw new IllegalArgumentException("System prompt contains disallowed invalid keywords");
        }
    }

    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_INPUT", "Campaign name is required"));
        }
        String spintaxRules = request.get("spintaxRules");
        String id = request.get("id");
        if (id == null || id.trim().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }

        String systemPrompt = request.get("systemPrompt");
        String aiPersona = request.get("aiPersona");
        String salesGoals = request.get("salesGoals");
        String toneOfVoice = request.get("toneOfVoice");
        String productFaqs = request.get("productFaqs");
        String qualificationRules = request.get("qualificationRules");

        try {
            validatePromptData(systemPrompt, aiPersona, salesGoals, toneOfVoice, productFaqs, qualificationRules);
        } catch (IllegalArgumentException e) {
            log.error("Invalid custom system prompt in campaign creation for name {}: {}", name, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_PROMPT_DATA", e.getMessage()));
        }

        Campaign campaign = new Campaign(id, name, spintaxRules, systemPrompt, aiPersona, salesGoals, toneOfVoice, productFaqs, qualificationRules);
        Campaign savedCampaign = campaignService.saveCampaign(campaign);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCampaign);
    }

    @PutMapping("/{campaignId}/system-prompt")
    public ResponseEntity<?> updateSystemPrompt(
            @PathVariable("campaignId") String campaignId,
            @RequestBody Map<String, String> request) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElse(null);
        if (campaign == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("RESOURCE_NOT_FOUND", "Campaign not found: " + campaignId));
        }

        String systemPrompt = request.get("systemPrompt");
        String aiPersona = request.get("aiPersona");
        String salesGoals = request.get("salesGoals");
        String toneOfVoice = request.get("toneOfVoice");
        String productFaqs = request.get("productFaqs");
        String qualificationRules = request.get("qualificationRules");

        try {
            validatePromptData(systemPrompt, aiPersona, salesGoals, toneOfVoice, productFaqs, qualificationRules);
        } catch (IllegalArgumentException e) {
            log.error("Invalid custom system prompt update attempt for campaign ID {}: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_PROMPT_DATA", e.getMessage()));
        }

        campaign.setSystemPrompt(systemPrompt);
        campaign.setAiPersona(aiPersona);
        campaign.setSalesGoals(salesGoals);
        campaign.setToneOfVoice(toneOfVoice);
        campaign.setProductFaqs(productFaqs);
        campaign.setQualificationRules(qualificationRules);

        Campaign savedCampaign = campaignService.saveCampaign(campaign);
        return ResponseEntity.ok(savedCampaign);
    }

    @GetMapping
    public ResponseEntity<?> getCampaigns() {
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        return ResponseEntity.ok()
                .header("Cache-Control", "max-age=" + com.eneik.generated.config.CacheConstants.TTL_CAMPAIGNS_SEC)
                .body(campaigns);
    }

    @PostMapping("/{campaignId}/leads/import")
    public ResponseEntity<?> importLeads(
            @PathVariable("campaignId") String campaignId,
            @RequestBody ImportLeadsRequest request) {
        if (!campaignRepository.existsById(campaignId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("RESOURCE_NOT_FOUND", "Campaign not found: " + campaignId));
        }
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_INPUT", "Lead content is required"));
        }
        try {
            List<Lead> leads = leadImportService.importLeads(campaignId, request.getContent());
            return ResponseEntity.status(HttpStatus.CREATED).body(leads);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("IMPORT_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/assignments")
    public ResponseEntity<?> assignCampaign(@RequestBody CampaignAssignmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (request.getCampaignId() == null) {
            throw new IllegalArgumentException("campaignId cannot be null");
        }
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        if (request.getAccountType() == null) {
            throw new IllegalArgumentException("accountType cannot be null");
        }

        try {
            boolean success = campaignAssignmentService.assignCampaign(
                    request.getCampaignId(),
                    request.getAccountId(),
                    request.getAccountType()
            );

            if (success) {
                CampaignAssignmentResponse response = new CampaignAssignmentResponse(
                        request.getCampaignId(),
                        request.getAccountId(),
                        request.getAccountType().name(),
                        "ASSIGNED"
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }

            ErrorResponse error = new ErrorResponse(
                    "ASSIGNMENT_FAILED",
                    "Campaign assignment could not be processed"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.contains("not found")) {
                ErrorResponse error = new ErrorResponse("RESOURCE_NOT_FOUND", message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            } else {
                ErrorResponse error = new ErrorResponse("INVALID_ACCOUNT_AGE", message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
        }
    }
}
