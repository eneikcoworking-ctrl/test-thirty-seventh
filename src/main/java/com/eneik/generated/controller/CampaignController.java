package com.eneik.generated.controller;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.domain.Lead;
import com.eneik.generated.service.CampaignService;
import com.eneik.generated.service.LeadImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final LeadImportService leadImportService;

    public CampaignController(CampaignService campaignService, LeadImportService leadImportService) {
        this.campaignService = campaignService;
        this.leadImportService = leadImportService;
    }

    @PostMapping
    public ResponseEntity<Campaign> saveCampaign(@RequestBody Campaign campaign) {
        if (campaign.getId() == null) {
            campaign.setId(java.util.UUID.randomUUID().toString());
        }
        Campaign saved = campaignService.saveCampaign(campaign);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Campaign> getCampaign(@PathVariable String id) {
        return campaignService.getCampaign(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/leads/import")
    public ResponseEntity<List<Lead>> importLeads(
            @PathVariable String id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) String rawBody) {

        // Check if campaign exists first
        if (campaignService.getCampaign(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String content = "";
        if (file != null && !file.isEmpty()) {
            try {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else if (rawBody != null) {
            content = rawBody;
        }

        List<Lead> imported = leadImportService.importLeads(id, content);
        return ResponseEntity.ok(imported);
    }
}
