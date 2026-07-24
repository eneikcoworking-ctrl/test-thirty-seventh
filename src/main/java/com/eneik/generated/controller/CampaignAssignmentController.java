package com.eneik.generated.controller;

import com.eneik.generated.dto.CampaignAssignmentRequest;
import com.eneik.generated.dto.CampaignAssignmentResponse;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.service.CampaignAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignAssignmentController {

    private final CampaignAssignmentService campaignAssignmentService;

    public CampaignAssignmentController(CampaignAssignmentService campaignAssignmentService) {
        this.campaignAssignmentService = campaignAssignmentService;
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
