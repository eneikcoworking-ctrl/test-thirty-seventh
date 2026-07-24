package com.eneik.generated.controller;

import com.eneik.generated.dto.DispatchRequest;
import com.eneik.generated.dto.DispatchResponse;
import com.eneik.generated.dto.ErrorResponse;
import com.eneik.generated.exception.DailyLimitExceededException;
import com.eneik.generated.exception.SessionLimitReachedException;
import com.eneik.generated.service.MessageDispatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageDispatchService messageDispatchService;

    public MessageController(MessageDispatchService messageDispatchService) {
        this.messageDispatchService = messageDispatchService;
    }

    @PostMapping("/dispatch")
    public ResponseEntity<?> dispatchMessage(@RequestBody DispatchRequest request) {
        try {
            messageDispatchService.dispatchMessage(
                    request.getTgAccountId(),
                    request.getLeadIdentifier(),
                    request.getMessage()
            );
            return ResponseEntity.ok(new DispatchResponse("SUCCESS", "Message dispatched successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (DailyLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("DAILY_LIMIT_EXCEEDED", e.getMessage()));
        } catch (SessionLimitReachedException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("SESSION_LIMIT_REACHED", e.getMessage()));
        }
    }

    @PostMapping("/receive")
    public ResponseEntity<?> receiveMessage(@RequestBody DispatchRequest request) {
        try {
            messageDispatchService.receiveMessage(
                    request.getTgAccountId(),
                    request.getLeadIdentifier(),
                    request.getMessage()
            );
            return ResponseEntity.ok(new DispatchResponse("SUCCESS", "Incoming message processed successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }
}
