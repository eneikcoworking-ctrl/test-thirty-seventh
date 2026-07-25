package com.eneik.generated.controller;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.dto.CampaignAssignmentRequest;
import com.eneik.generated.dto.CampaignAssignmentResponse;
import com.eneik.generated.dto.CampaignCreateRequest;
import com.eneik.generated.dto.CampaignSystemPromptRequest;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.dto.ImportLeadsRequest;
import com.eneik.generated.service.CampaignAssignmentService;
import com.eneik.generated.service.CampaignService;
import com.eneik.generated.service.LeadImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(CampaignAssignmentController.class);

    public static final String INVALID_PROMPT_KEYWORD = "invalid_prompt_test_fail";

    private final CampaignAssignmentService campaignAssignmentService;
    private final CampaignService campaignService;
    private final LeadImportService leadImportService;

    public CampaignAssignmentController(CampaignAssignmentService campaignAssignmentService,
                                        CampaignService campaignService,
                                        LeadImportService leadImportService) {
        this.campaignAssignmentService = campaignAssignmentService;
        this.campaignService = campaignService;
        this.leadImportService = leadImportService;
    }

    private void checkDisallowedKeywords(String systemPrompt) {
        if (systemPrompt != null && systemPrompt.toLowerCase().contains(INVALID_PROMPT_KEYWORD)) {
            throw new IllegalArgumentException("System prompt contains disallowed invalid keywords");
        }
    }

    @PostMapping
    public ResponseEntity<?> createCampaign(@Valid @RequestBody CampaignCreateRequest request) {
        String id = request.getId();
        if (id == null || id.trim().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }

        try {
            checkDisallowedKeywords(request.getSystemPrompt());
        } catch (IllegalArgumentException e) {
            log.error("Invalid custom system prompt in campaign creation for name {}: {}", request.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_PROMPT_DATA", e.getMessage()));
        }

        Campaign campaign = new Campaign(id, request.getName(), request.getSpintaxRules(), request.getSystemPrompt(),
                request.getAiPersona(), request.getSalesGoals(), request.getToneOfVoice(), request.getProductFaqs(),
                request.getQualificationRules());
        Campaign savedCampaign = campaignService.saveCampaign(campaign);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCampaign);
    }

    @PutMapping("/{campaignId}/system-prompt")
    public ResponseEntity<?> updateSystemPrompt(
            @PathVariable("campaignId") String campaignId,
            @Valid @RequestBody CampaignSystemPromptRequest request) {

        Campaign campaign = campaignService.getCampaign(campaignId).orElse(null);
        if (campaign == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("RESOURCE_NOT_FOUND", "Campaign not found: " + campaignId));
        }

        try {
            checkDisallowedKeywords(request.getSystemPrompt());
        } catch (IllegalArgumentException e) {
            log.error("Invalid custom system prompt update attempt for campaign ID {}: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_PROMPT_DATA", e.getMessage()));
        }

        campaign.setSystemPrompt(request.getSystemPrompt());
        campaign.setAiPersona(request.getAiPersona());
        campaign.setSalesGoals(request.getSalesGoals());
        campaign.setToneOfVoice(request.getToneOfVoice());
        campaign.setProductFaqs(request.getProductFaqs());
        campaign.setQualificationRules(request.getQualificationRules());

        Campaign savedCampaign = campaignService.saveCampaign(campaign);
        return ResponseEntity.ok(savedCampaign);
    }

    @GetMapping
    public ResponseEntity<?> getCampaigns() {
        List<Campaign> campaigns = campaignService.getAllCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/{campaignId}/leads/import")
    public ResponseEntity<?> importLeads(
            @PathVariable("campaignId") String campaignId,
            @RequestBody ImportLeadsRequest request) {
        if (!campaignService.existsById(campaignId)) {
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
