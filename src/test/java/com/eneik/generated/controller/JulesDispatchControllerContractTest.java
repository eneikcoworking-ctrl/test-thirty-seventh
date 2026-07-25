package com.eneik.generated.controller;

import com.eneik.generated.Application;
import com.eneik.generated.domain.TaskStatus;
import com.eneik.generated.dto.CreateTaskRequest;
import com.eneik.generated.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JulesDispatchControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Contract Test 1: Verify that the OpenAPI specification file exists
     * and contains the contract definitions for the `/api/v1/tasks` endpoints.
     */
    @Test
    public void testOpenApiContractSpecificationFile() throws Exception {
        File openApiFile = new File("docs/openapi.yaml");
        assertTrue(openApiFile.exists(), "docs/openapi.yaml API contract specification must exist.");

        String specContent = new String(Files.readAllBytes(Paths.get("docs/openapi.yaml")));

        // Verify key paths exist in the spec content
        assertTrue(specContent.contains("/api/v1/tasks:"), "API spec must define /api/v1/tasks path.");
        assertTrue(specContent.contains("/api/v1/tasks/{id}:"), "API spec must define /api/v1/tasks/{id} path.");
        assertTrue(specContent.contains("/api/v1/tasks/{id}/submit:"), "API spec must define /api/v1/tasks/{id}/submit path.");
        assertTrue(specContent.contains("/api/v1/tasks/{id}/reject:"), "API spec must define /api/v1/tasks/{id}/reject path.");
        assertTrue(specContent.contains("/api/v1/tasks/{id}/evaluate:"), "API spec must define /api/v1/tasks/{id}/evaluate path.");

        // Verify schemas exist in the spec content
        assertTrue(specContent.contains("CreateTaskRequest:"), "API spec must define CreateTaskRequest schema.");
        assertTrue(specContent.contains("TaskDto:"), "API spec must define TaskDto schema.");
    }

    /**
     * Contract Test 2: Verify endpoint behavior against the specified API Contract.
     * This ensures that our Java implementation matches the request/response payloads,
     * status codes, and fields defined in docs/openapi.yaml.
     */
    @Test
    public void testTasksEndpointContracts() throws Exception {
        String taskId = UUID.randomUUID().toString();
        String sessionId = "session_contract_999";

        CreateTaskRequest createRequest = new CreateTaskRequest(taskId, sessionId);

        // 1. POST /api/v1/tasks matches the spec schema (input: CreateTaskRequest, output: TaskDto)
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(taskId)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.rejectionCount", is(0)))
                .andExpect(jsonPath("$.sessionId", is(sessionId)));

        // 2. GET /api/v1/tasks/{id} matches the spec schema (output: TaskDto)
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.rejectionCount", is(0)))
                .andExpect(jsonPath("$.sessionId", is(sessionId)));

        // 3. POST /api/v1/tasks/{id}/submit matches the spec (status: 200)
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/submit"))
                .andExpect(status().isOk());

        // Get and check status is UNDER_REVIEW
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNDER_REVIEW")));

        // 4. POST /api/v1/tasks/{id}/reject matches the spec (status: 200)
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/reject"))
                .andExpect(status().isOk());

        // Get and check status is REVIEW_REJECTED and rejectionCount is 1
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVIEW_REJECTED")))
                .andExpect(jsonPath("$.rejectionCount", is(1)));

        // 5. POST /api/v1/tasks/{id}/evaluate matches the spec (status: 200)
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/evaluate"))
                .andExpect(status().isOk());

        // Under-threshold evaluation should transition task back to IN_PROGRESS
        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    /**
     * Contract Test 3: Validate negative contract scenarios (e.g. 404 Not Found)
     */
    @Test
    public void testTaskEndpointsNotFoundContracts() throws Exception {
        String nonExistentId = "non-existent-task-id";

        // GET should return 404 Not Found
        mockMvc.perform(get("/api/v1/tasks/" + nonExistentId))
                .andExpect(status().isNotFound());

        // POST submit should return 404 Not Found
        mockMvc.perform(post("/api/v1/tasks/" + nonExistentId + "/submit"))
                .andExpect(status().isNotFound());

        // POST reject should return 404 Not Found
        mockMvc.perform(post("/api/v1/tasks/" + nonExistentId + "/reject"))
                .andExpect(status().isNotFound());

        // POST evaluate should return 404 Not Found
        mockMvc.perform(post("/api/v1/tasks/" + nonExistentId + "/evaluate"))
                .andExpect(status().isNotFound());
    }
}
