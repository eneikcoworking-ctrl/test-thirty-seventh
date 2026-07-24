package com.eneik.generated.leadgen.controller;

import com.eneik.generated.leadgen.model.Conversation;
import com.eneik.generated.leadgen.model.ConversationMessage;
import com.eneik.generated.leadgen.service.InboxService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class InboxController {

    private final InboxService inboxService;

    public InboxController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @RequestParam(name = "escalationStatus", required = false, defaultValue = "ALL") String escalationStatus,
            @RequestParam(name = "assignedAgentId", required = false) String assignedAgentId,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page) {
        try {
            Page<Conversation> convPage = inboxService.getConversations(escalationStatus, assignedAgentId, page, limit);

            List<ConversationDto> dtos = convPage.getContent().stream()
                    .map(c -> new ConversationDto(
                            c.getId(),
                            c.getTelegramChatId(),
                            c.getLeadName(),
                            c.getLeadUsername(),
                            c.getLeadPhone(),
                            c.getStatus(),
                            c.getAssignedAgentId(),
                            c.getLastMessageAt(),
                            c.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            ConversationPageDto response = new ConversationPageDto(
                    dtos,
                    convPage.getTotalElements(),
                    convPage.getTotalPages(),
                    convPage.getNumber(),
                    convPage.getSize()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable(name = "conversationId") String conversationId,
            @RequestParam(name = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(name = "beforeMessageId", required = false) String beforeMessageId) {
        try {
            List<ConversationMessage> messages = inboxService.getMessages(conversationId, limit, beforeMessageId);
            List<MessageDto> dtos = messages.stream()
                    .map(m -> new MessageDto(
                            m.getId(),
                            m.getConversationId(),
                            m.getText(),
                            m.getSenderType(),
                            m.getSentAt(),
                            m.getSenderName()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> sendManualMessage(
            @PathVariable(name = "conversationId") String conversationId,
            @RequestBody SendMessageRequestDto request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            ErrorResponseDto error = new ErrorResponseDto("INVALID_ARGUMENT", "Message text must not be empty", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            ConversationMessage message = inboxService.sendManualMessage(conversationId, request.getText());
            MessageDto responseDto = new MessageDto(
                    message.getId(),
                    message.getConversationId(),
                    message.getText(),
                    message.getSenderType(),
                    message.getSentAt(),
                    message.getSenderName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            ErrorResponseDto error = new ErrorResponseDto("NOT_FOUND", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/conversations/{conversationId}/lead-messages")
    public ResponseEntity<?> receiveLeadMessage(
            @PathVariable(name = "conversationId") String conversationId,
            @RequestBody SendMessageRequestDto request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            ErrorResponseDto error = new ErrorResponseDto("INVALID_ARGUMENT", "Message text must not be empty", OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            ConversationMessage message = inboxService.receiveLeadMessage(conversationId, request.getText());
            MessageDto responseDto = new MessageDto(
                    message.getId(),
                    message.getConversationId(),
                    message.getText(),
                    message.getSenderType(),
                    message.getSentAt(),
                    message.getSenderName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            ErrorResponseDto error = new ErrorResponseDto("NOT_FOUND", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", e.getMessage(), OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
