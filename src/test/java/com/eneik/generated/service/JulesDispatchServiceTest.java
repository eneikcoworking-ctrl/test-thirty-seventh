package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.Task;
import com.eneik.generated.domain.TaskStatus;
import com.eneik.generated.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
public class JulesDispatchServiceTest {

    @Autowired
    private JulesDispatchService julesDispatchService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    public void testCreateAndSubmitTask() {
        String taskId = UUID.randomUUID().toString();
        String sessionId = "session_abc_123";

        // 1. Create a task
        Task task = julesDispatchService.createTask(taskId, sessionId);
        assertNotNull(task);
        assertEquals(taskId, task.getId());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(0, task.getRejectionCount());

        // 2. Submit for review
        julesDispatchService.submitForReview(taskId);

        Task updatedTask = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.UNDER_REVIEW, updatedTask.getStatus());
    }

    @Test
    public void testTaskLoopsRejectionBelowThreshold() {
        julesDispatchService.setMaxRejections(3);

        String taskId = UUID.randomUUID().toString();
        String sessionId = "session_xyz";

        // Create and start processing
        julesDispatchService.createTask(taskId, sessionId);

        // Transition: PENDING -> UNDER_REVIEW
        julesDispatchService.submitForReview(taskId);

        // First rejection: UNDER_REVIEW -> REVIEW_REJECTED (rejectionCount = 1)
        julesDispatchService.rejectTask(taskId);

        Task taskAfterReject = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.REVIEW_REJECTED, taskAfterReject.getStatus());
        assertEquals(1, taskAfterReject.getRejectionCount());

        // Evaluate status: since 1 < 3, should transition to IN_PROGRESS (normal processing)
        julesDispatchService.evaluateTaskStatus(taskId);

        Task taskAfterEval = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, taskAfterEval.getStatus(), "Should transition to IN_PROGRESS since rejection count is below threshold");
        assertEquals(1, taskAfterEval.getRejectionCount());
    }

    @Test
    public void testTaskLoopsRejectionExceedsThresholdTransitionsToTerminal() {
        julesDispatchService.setMaxRejections(3);

        String taskId = UUID.randomUUID().toString();
        String sessionId = "session_123";

        // Create task
        julesDispatchService.createTask(taskId, sessionId);

        // 1st cycle: PENDING -> UNDER_REVIEW -> REVIEW_REJECTED (rejectionCount = 1) -> IN_PROGRESS
        julesDispatchService.submitForReview(taskId);
        julesDispatchService.rejectTask(taskId);
        julesDispatchService.evaluateTaskStatus(taskId);

        Task task1 = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, task1.getStatus());
        assertEquals(1, task1.getRejectionCount());

        // 2nd cycle: IN_PROGRESS -> UNDER_REVIEW -> REVIEW_REJECTED (rejectionCount = 2) -> IN_PROGRESS
        julesDispatchService.submitForReview(taskId);
        julesDispatchService.rejectTask(taskId);
        julesDispatchService.evaluateTaskStatus(taskId);

        Task task2 = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, task2.getStatus());
        assertEquals(2, task2.getRejectionCount());

        // 3rd cycle: IN_PROGRESS -> UNDER_REVIEW -> REVIEW_REJECTED (rejectionCount = 3) -> TERMINAL
        julesDispatchService.submitForReview(taskId);
        julesDispatchService.rejectTask(taskId);
        julesDispatchService.evaluateTaskStatus(taskId);

        Task task3 = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.TERMINAL, task3.getStatus(), "Should transition to TERMINAL as rejection count has reached threshold of 3");
        assertEquals(3, task3.getRejectionCount());
    }

    @Test
    public void testAtomicGuardBoundaries() {
        String taskId = UUID.randomUUID().toString();
        String sessionId = "session_atomic";

        // Create task (status: PENDING)
        Task task = julesDispatchService.createTask(taskId, sessionId);
        assertEquals(TaskStatus.PENDING, task.getStatus());

        // Attempting to atomically transition status with INCORRECT expectedStatus must fail (modify 0 rows)
        int updated = taskRepository.updateStatusAtomically(taskId, TaskStatus.TERMINAL, TaskStatus.REVIEW_REJECTED);
        assertEquals(0, updated, "Atomic update should affect 0 rows because current status is PENDING, not REVIEW_REJECTED");

        // Verify task status is still PENDING
        Task taskAfterFailedUpdate = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.PENDING, taskAfterFailedUpdate.getStatus());

        // Attempting with CORRECT expectedStatus must succeed (modify 1 row)
        int successfulUpdate = taskRepository.updateStatusAtomically(taskId, TaskStatus.IN_PROGRESS, TaskStatus.PENDING);
        assertEquals(1, successfulUpdate, "Atomic update should affect 1 row as current status matches PENDING");

        // Verify status has updated to IN_PROGRESS
        Task taskAfterSuccess = taskRepository.findById(taskId).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, taskAfterSuccess.getStatus());
    }
}
