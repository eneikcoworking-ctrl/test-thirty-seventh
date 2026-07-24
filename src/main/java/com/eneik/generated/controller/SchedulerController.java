package com.eneik.generated.controller;

import com.eneik.generated.dto.ActionDelayResponse;
import com.eneik.generated.dto.ActionRequest;
import com.eneik.generated.service.DelayCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private final DelayCalculationService delayCalculationService;

    public SchedulerController(DelayCalculationService delayCalculationService) {
        this.delayCalculationService = delayCalculationService;
    }

    @PostMapping("/delay")
    public ResponseEntity<ActionDelayResponse> computeDelay(@RequestBody ActionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        if (request.getActionType() == null || request.getActionType().trim().isEmpty()) {
            throw new IllegalArgumentException("actionType cannot be null or empty");
        }
        if (request.getMeanDelaySeconds() == null || request.getMeanDelaySeconds() <= 0) {
            throw new IllegalArgumentException("meanDelaySeconds must be positive");
        }
        double delay = delayCalculationService.calculateExponentialDelay(request.getMeanDelaySeconds());
        return ResponseEntity.ok(new ActionDelayResponse(delay));
    }
}
