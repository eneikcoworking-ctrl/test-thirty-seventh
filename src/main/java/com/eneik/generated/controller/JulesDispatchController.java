package com.eneik.generated.controller;

import com.eneik.generated.domain.Task;
import com.eneik.generated.domain.TaskStatus;
import com.eneik.generated.dto.CreateTaskRequest;
import com.eneik.generated.dto.TaskDto;
import com.eneik.generated.service.JulesDispatchService;
import com.eneik.generated.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class JulesDispatchController {

    private static final Logger log = LoggerFactory.getLogger(JulesDispatchController.class);

    private final JulesDispatchService julesDispatchService;
    private final TaskRepository taskRepository;

    public JulesDispatchController(JulesDispatchService julesDispatchService, TaskRepository taskRepository) {
        this.julesDispatchService = julesDispatchService;
        this.taskRepository = taskRepository;
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody CreateTaskRequest request) {
        log.info("REST request to create task: {}", request.getId());
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Task task = julesDispatchService.createTask(request.getId(), request.getSessionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(task));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTask(@PathVariable String id) {
        log.info("REST request to get task: {}", id);
        return taskRepository.findById(id)
                .map(task -> ResponseEntity.ok(toDto(task)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitForReview(@PathVariable String id) {
        log.info("REST request to submit task for review: {}", id);
        try {
            julesDispatchService.submitForReview(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Task not found for submission: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectTask(@PathVariable String id) {
        log.info("REST request to reject task: {}", id);
        try {
            julesDispatchService.rejectTask(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Task not found for rejection: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/evaluate")
    public ResponseEntity<Void> evaluateTask(@PathVariable String id) {
        log.info("REST request to evaluate task status: {}", id);
        try {
            julesDispatchService.evaluateTaskStatus(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Task not found for evaluation: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    private TaskDto toDto(Task task) {
        return new TaskDto(
                task.getId(),
                mapTaskStatusToString(task.getStatus()),
                task.getRejectionCount(),
                task.getSessionId()
        );
    }

    /**
     * Helper mapper that maps all enum values of TaskStatus exhaustively to string.
     */
    private String mapTaskStatusToString(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("TaskStatus cannot be null");
        }
        return switch (status) {
            case PENDING -> "PENDING";
            case IN_PROGRESS -> "IN_PROGRESS";
            case UNDER_REVIEW -> "UNDER_REVIEW";
            case REVIEW_REJECTED -> "REVIEW_REJECTED";
            case COMPLETED -> "COMPLETED";
            case TERMINAL -> "TERMINAL";
        };
    }
}
