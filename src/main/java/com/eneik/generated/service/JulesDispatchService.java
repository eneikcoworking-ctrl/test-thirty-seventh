package com.eneik.generated.service;

import com.eneik.generated.domain.Task;
import com.eneik.generated.domain.TaskStatus;
import com.eneik.generated.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JulesDispatchService {

    private static final Logger log = LoggerFactory.getLogger(JulesDispatchService.class);

    private final TaskRepository taskRepository;

    @Value("${app.dispatch.max-rejections:3}")
    private int maxRejections = 3;

    public JulesDispatchService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public int getMaxRejections() {
        return maxRejections;
    }

    public void setMaxRejections(int maxRejections) {
        this.maxRejections = maxRejections;
    }

    /**
     * Creates a new task in PENDING state.
     */
    public Task createTask(String id, String sessionId) {
        log.info("Creating task: {} for session: {}", id, sessionId);
        Task task = new Task(id, TaskStatus.PENDING, sessionId);
        return taskRepository.save(task);
    }

    /**
     * Atomically transition a task to UNDER_REVIEW from PENDING or IN_PROGRESS.
     */
    public void submitForReview(String id) {
        log.info("Submitting task: {} for review", id);
        Task task = getTaskOrThrow(id);
        if (task.getStatus().isTerminal()) {
            log.warn("Cannot submit terminal task {} for review", id);
            return;
        }

        int updated = taskRepository.updateStatusAtomically(id, TaskStatus.UNDER_REVIEW, task.getStatus());
        if (updated == 0) {
            log.warn("Atomic update failed for submitting task {} for review", id);
        } else {
            log.info("Task {} successfully submitted for review", id);
        }
    }

    /**
     * Atomically transition a task to REVIEW_REJECTED and increment its rejection count.
     */
    public void rejectTask(String id) {
        log.info("Rejecting task: {}", id);
        Task task = getTaskOrThrow(id);
        if (task.getStatus().isTerminal()) {
            log.warn("Cannot reject terminal task {}", id);
            return;
        }

        int updated = taskRepository.rejectAndIncrementAtomically(id, TaskStatus.REVIEW_REJECTED, task.getStatus());
        if (updated == 0) {
            log.warn("Atomic update failed for rejecting task {}", id);
        } else {
            log.info("Task {} successfully marked as REVIEW_REJECTED. New rejection count: {}", id, task.getRejectionCount() + 1);
        }
    }

    /**
     * Evaluates the task status.
     * Enforces the rejection threshold check to break the rejection loop.
     */
    public void evaluateTaskStatus(String id) {
        log.info("Evaluating status for task: {}", id);
        Task task = getTaskOrThrow(id);

        if (task.getStatus().isTerminal()) {
            log.info("Task {} is already in terminal state: {}", id, task.getStatus());
            return;
        }

        if (task.getStatus() == TaskStatus.REVIEW_REJECTED) {
            int currentRejections = task.getRejectionCount();
            log.info("Task {} has been rejected {} times (threshold: {})", id, currentRejections, maxRejections);

            if (currentRejections >= maxRejections) {
                log.warn("Task {} exceeded rejection threshold of {}. Breaking loop by transitioning to TERMINAL status.", id, maxRejections);
                int updated = taskRepository.updateStatusAtomically(id, TaskStatus.TERMINAL, TaskStatus.REVIEW_REJECTED);
                if (updated == 0) {
                    log.error("Failed atomic status update to TERMINAL for task: {}", id);
                } else {
                    log.info("Task {} transitioned to TERMINAL state successfully.", id);
                }
            } else {
                log.info("Task {} is below rejection threshold. Transitioning back to IN_PROGRESS for normal processing.", id);
                int updated = taskRepository.updateStatusAtomically(id, TaskStatus.IN_PROGRESS, TaskStatus.REVIEW_REJECTED);
                if (updated == 0) {
                    log.error("Failed atomic status update to IN_PROGRESS for task: {}", id);
                } else {
                    log.info("Task {} transitioned back to IN_PROGRESS state successfully.", id);
                }
            }
        } else {
            log.info("Task {} is in status {} - no threshold check needed.", id, task.getStatus());
        }
    }

    private Task getTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
    }
}
