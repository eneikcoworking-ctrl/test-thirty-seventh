package com.eneik.generated.controller;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.service.DialogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class ConversationController {

    private final DialogService dialogService;

    public ConversationController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    /**
     * Endpoint to fetch paginated Dialog list.
     */
    @GetMapping("/dialogs")
    public ResponseEntity<Page<DialogResponseDto>> getAllDialogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {

        // Enforce pagination constraints
        Pageable pageable = PageRequest.of(page, limit);
        Page<Dialog> dialogsPage = dialogService.findAllDialogs(pageable);

        Page<DialogResponseDto> responsePage = dialogsPage.map(dialog -> new DialogResponseDto(
                dialog.getId(),
                dialog.getTelegramChatId(),
                mapAiStateToString(dialog.getAiState()),
                dialog.getCreatedAt(),
                dialog.getUpdatedAt()
        ));

        return ResponseEntity.ok(responsePage);
    }

    /**
     * Endpoint to fetch a single Dialog by ID.
     */
    @GetMapping("/dialogs/{id}")
    public ResponseEntity<DialogResponseDto> getDialogById(@PathVariable(name = "id") Long id) {
        return dialogService.findDialogById(id)
                .map(dialog -> ResponseEntity.ok(new DialogResponseDto(
                        dialog.getId(),
                        dialog.getTelegramChatId(),
                        mapAiStateToString(dialog.getAiState()),
                        dialog.getCreatedAt(),
                        dialog.getUpdatedAt()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Helper mapper that maps all enum values of AiState exhaustively to string.
     * Addressing Concern #2: Ensure every potential enum value is checked and handled.
     */
    public String mapAiStateToString(AiState state) {
        if (state == null) {
            throw new IllegalArgumentException("AiState cannot be null");
        }
        switch (state) {
            case ACTIVE:
                return "ACTIVE";
            case STOPPED:
                return "STOPPED";
            case PAUSED:
                return "PAUSED";
            default:
                throw new IllegalStateException("Unhandled AiState enum value: " + state);
        }
    }

    // Response DTO
    public static class DialogResponseDto {
        private Long id;
        private String telegramChatId;
        private String aiState;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public DialogResponseDto() {
        }

        public DialogResponseDto(Long id, String telegramChatId, String aiState, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.telegramChatId = telegramChatId;
            this.aiState = aiState;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(String telegramChatId) {
            this.telegramChatId = telegramChatId;
        }

        public String getAiState() {
            return aiState;
        }

        public void setAiState(String aiState) {
            this.aiState = aiState;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
