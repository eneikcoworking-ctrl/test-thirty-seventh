package com.eneik.generated.controller;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.model.SenderType;
import com.eneik.generated.service.DialogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final DialogService dialogService;

    public ConversationController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    /**
     * GET /api/v1/conversations
     * Fetch conversations. Returns a paginated list of conversations.
     */
    @GetMapping
    public ResponseEntity<ConversationPage> fetchConversations(
            @RequestParam(required = false, defaultValue = "ALL") String escalationStatus,
            @RequestParam(required = false) String assignedAgentId,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int page) {

        List<Dialog> dialogs = dialogService.findAllDialogs();
        List<ConversationDto> content = dialogs.stream()
                .map(this::mapToConversationDto)
                .collect(Collectors.toList());

        ConversationPage response = new ConversationPage();
        response.setContent(content);
        response.setTotalElements(content.size());
        response.setTotalPages(1);
        response.setPage(page);
        response.setSize(limit);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/conversations/{conversationId}/messages
     * Retrieve messages for a conversation.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDto>> retrieveMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String beforeMessageId) {

        return dialogService.findDialogById(conversationId)
                .map(dialog -> {
                    List<MessageDto> messages = dialog.getMessages().stream()
                            .map(m -> mapToMessageDto(m, conversationId))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(messages);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/conversations/{conversationId}/messages
     * Send a manual message (updates conversation status to PAUSED).
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageDto> sendManualMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {

        try {
            Message message = dialogService.sendManualMessage(conversationId, request.getText());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToMessageDto(message, conversationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/v1/conversations/{conversationId}/lead-messages
     * Endpoint to simulate receiving a lead reply (ignores if PAUSED, auto-replies if ACTIVE).
     */
    @PostMapping("/{conversationId}/lead-messages")
    public ResponseEntity<MessageDto> postLeadMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {

        try {
            Message message = dialogService.receiveLeadMessage(conversationId, request.getText());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToMessageDto(message, conversationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Helper Mappers & DTOs ---

    private ConversationDto mapToConversationDto(Dialog dialog) {
        ConversationDto dto = new ConversationDto();
        dto.setId(String.valueOf(dialog.getId()));
        dto.setTelegramChatId(dialog.getTelegramChatId());
        dto.setLeadName("Lead @ " + dialog.getTelegramChatId());
        dto.setLeadUsername(dialog.getTelegramChatId());
        dto.setLeadPhone(null);

        // Map AiState to the OpenAPI status enum: [ACTIVE, ESCALATED, RESOLVED, PAUSED]
        if (dialog.getAiState() == AiState.ACTIVE) {
            dto.setStatus("ACTIVE");
        } else if (dialog.getAiState() == AiState.PAUSED) {
            dto.setStatus("PAUSED");
        } else {
            dto.setStatus("RESOLVED");
        }

        dto.setAssignedAgentId(null);

        String isoTime = dialog.getUpdatedAt() != null ?
                dialog.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) :
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        dto.setLastMessageAt(isoTime);
        dto.setCreatedAt(dialog.getCreatedAt() != null ?
                dialog.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) :
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return dto;
    }

    private MessageDto mapToMessageDto(Message msg, Long conversationId) {
        MessageDto dto = new MessageDto();
        dto.setId(String.valueOf(msg.getId()));
        dto.setConversationId(String.valueOf(conversationId));
        dto.setText(msg.getText());

        // Map model's SenderType to OpenAPI specification: [LEAD, AI_AGENT, HUMAN_REPRESENTATIVE]
        if (msg.getSenderType() == SenderType.USER) {
            dto.setSenderType("LEAD");
            dto.setSenderName("Lead");
        } else if (msg.getSenderType() == SenderType.AI) {
            dto.setSenderType("AI_AGENT");
            dto.setSenderName("AI Bot");
        } else if (msg.getSenderType() == SenderType.HUMAN_REPRESENTATIVE) {
            dto.setSenderType("HUMAN_REPRESENTATIVE");
            dto.setSenderName("Human Representative");
        } else {
            dto.setSenderType("LEAD");
            dto.setSenderName("System/Other");
        }

        String isoTime = msg.getReceivedAt() != null ?
                msg.getReceivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) :
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        dto.setSentAt(isoTime);
        return dto;
    }

    public static class SendMessageRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class ConversationPage {
        private List<ConversationDto> content = new ArrayList<>();
        private int totalElements;
        private int totalPages;
        private int page;
        private int size;

        public List<ConversationDto> getContent() {
            return content;
        }

        public void setContent(List<ConversationDto> content) {
            this.content = content;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(int totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    public static class ConversationDto {
        private String id;
        private String telegramChatId;
        private String leadName;
        private String leadUsername;
        private String leadPhone;
        private String status;
        private String assignedAgentId;
        private String lastMessageAt;
        private String createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(String telegramChatId) {
            this.telegramChatId = telegramChatId;
        }

        public String getLeadName() {
            return leadName;
        }

        public void setLeadName(String leadName) {
            this.leadName = leadName;
        }

        public String getLeadUsername() {
            return leadUsername;
        }

        public void setLeadUsername(String leadUsername) {
            this.leadUsername = leadUsername;
        }

        public String getLeadPhone() {
            return leadPhone;
        }

        public void setLeadPhone(String leadPhone) {
            this.leadPhone = leadPhone;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAssignedAgentId() {
            return assignedAgentId;
        }

        public void setAssignedAgentId(String assignedAgentId) {
            this.assignedAgentId = assignedAgentId;
        }

        public String getLastMessageAt() {
            return lastMessageAt;
        }

        public void setLastMessageAt(String lastMessageAt) {
            this.lastMessageAt = lastMessageAt;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class MessageDto {
        private String id;
        private String conversationId;
        private String text;
        private String senderType;
        private String sentAt;
        private String senderName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSenderType() {
            return senderType;
        }

        public void setSenderType(String senderType) {
            this.senderType = senderType;
        }

        public String getSentAt() {
            return sentAt;
        }

        public void setSentAt(String sentAt) {
            this.sentAt = sentAt;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }
    }
}
