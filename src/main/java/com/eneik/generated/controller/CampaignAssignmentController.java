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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignAssignmentController {

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
        Campaign campaign = new Campaign(id, name, spintaxRules);
        Campaign savedCampaign = campaignService.saveCampaign(campaign);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCampaign);
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
