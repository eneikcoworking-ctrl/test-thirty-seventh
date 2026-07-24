package com.eneik.generated.leadgen.controller;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.Message;
import com.eneik.generated.leadgen.service.InboxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final InboxService inboxService;

    public InboxController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping("/chats")
    public ResponseEntity<?> getInboxChats() {
        try {
            List<Conversation> conversations = inboxService.getAllConversations();
            List<ConversationDto> dtos = conversations.stream()
                    .map(c -> new ConversationDto(
                            c.getId(),
                            c.getTelegramAccountId(),
                            c.getLeadId(),
                            c.getLeadUsername(),
                            c.getLastMessage(),
                            c.getLastMessageTimestamp()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendManualMessage(@RequestBody ManualMessageRequestDto request) {
        if (request.getTelegramAccountId() == null || request.getTelegramAccountId().trim().isEmpty()) {
            ErrorResponseDto error = new ErrorResponseDto("INVALID_ARGUMENT", "telegramAccountId is required", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (request.getLeadId() == null || request.getLeadId().trim().isEmpty()) {
            ErrorResponseDto error = new ErrorResponseDto("INVALID_ARGUMENT", "leadId is required", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            ErrorResponseDto error = new ErrorResponseDto("INVALID_ARGUMENT", "message is required", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            Message message = inboxService.dispatchManualMessage(
                    request.getTelegramAccountId(),
                    request.getLeadId(),
                    request.getMessage()
            );

            MessageResponseDto responseDto = new MessageResponseDto(
                    String.valueOf(message.getId()),
                    message.getTelegramAccountId(),
                    message.getLeadId(),
                    message.getContent(),
                    message.getStatus(),
                    message.getTimestamp()
            );
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
