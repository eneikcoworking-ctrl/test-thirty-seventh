package com.eneik.generated.controller;

import com.eneik.generated.dto.CampaignDispatchRequest;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.service.CampaignFailoverService;
import com.eneik.generated.service.CampaignFailoverService.DispatchResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignDispatchController {

    private final CampaignFailoverService campaignFailoverService;

    public CampaignDispatchController(CampaignFailoverService campaignFailoverService) {
        this.campaignFailoverService = campaignFailoverService;
    }

    @PostMapping("/{campaignId}/dispatch")
    public ResponseEntity<?> dispatchCampaignMessage(
            @PathVariable("campaignId") String campaignId,
            @RequestBody CampaignDispatchRequest request) {

        if (campaignId == null || campaignId.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign ID must not be null or empty");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (request.getTelegramChatId() == null) {
            throw new IllegalArgumentException("telegramChatId must not be null");
        }
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("text must not be null or empty");
        }

        try {
            DispatchResult result = campaignFailoverService.dispatchWithFailover(
                    campaignId,
                    request.getTelegramChatId(),
                    request.getText()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                ErrorResponse error = new ErrorResponse(
                        "DISPATCH_FAILED",
                        result.getError() != null ? result.getError() : "Failed to dispatch message via failover"
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

        } catch (IllegalStateException e) {
            ErrorResponse error = new ErrorResponse("NO_ELIGIBLE_ACCOUNTS", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
