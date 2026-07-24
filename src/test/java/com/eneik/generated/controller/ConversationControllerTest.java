package com.eneik.generated.controller;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.service.DialogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private DialogService dialogService;

    @Autowired
    private ConversationController conversationController;

    @BeforeEach
    public void setup() {
        dialogRepository.deleteAll();
    }

    @Test
    public void testMapAiStateToString_ExhaustiveMapping() {
        // Explicitly assert that all enum states of AiState are correctly handled
        assertThat(conversationController.mapAiStateToString(AiState.ACTIVE)).isEqualTo("ACTIVE");
        assertThat(conversationController.mapAiStateToString(AiState.STOPPED)).isEqualTo("STOPPED");
        assertThat(conversationController.mapAiStateToString(AiState.PAUSED)).isEqualTo("PAUSED");

        // Null check
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            conversationController.mapAiStateToString(null);
        });
    }

    @Test
    public void testFindAllDialogs_PaginationAndMaximumConstraints() {
        // Create 60 Dialogs
        for (int i = 0; i < 60; i++) {
            Dialog d = new Dialog("chat_" + i, AiState.ACTIVE);
            dialogRepository.save(d);
        }

        // Test dialogService.findAllDialogs with no pagination or default
        var pageDefault = dialogService.findAllDialogs(null);
        assertThat(pageDefault.getSize()).isEqualTo(50); // Enforced maximum of 50

        // Test with page size greater than 50
        Pageable excessivePageable = PageRequest.of(0, 100);
        var pageEnforced = dialogService.findAllDialogs(excessivePageable);
        assertThat(pageEnforced.getSize()).isEqualTo(50); // Clamped to 50

        // Test standard acceptable page size
        Pageable normalPageable = PageRequest.of(0, 20);
        var pageNormal = dialogService.findAllDialogs(normalPageable);
        assertThat(pageNormal.getSize()).isEqualTo(20);
    }

    @Test
    public void testGetDialogs_EndpointPagination() throws Exception {
        // Create 5 Dialogs
        for (int i = 0; i < 5; i++) {
            Dialog d = new Dialog("chat_" + i, AiState.values()[i % AiState.values().length]);
            dialogRepository.save(d);
        }

        mockMvc.perform(get("/api/v1/dialogs")
                        .param("page", "0")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    public void testGetDialogById_Endpoint() throws Exception {
        Dialog d = new Dialog("chat_unique_id", AiState.PAUSED);
        Dialog saved = dialogRepository.save(d);

        mockMvc.perform(get("/api/v1/dialogs/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.telegramChatId", is("chat_unique_id")))
                .andExpect(jsonPath("$.aiState", is("PAUSED")));

        // Non-existent id
        mockMvc.perform(get("/api/v1/dialogs/999999"))
                .andExpect(status().isNotFound());
    }
}
