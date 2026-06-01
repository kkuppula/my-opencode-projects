package com.blackboard.ai.service;

import com.blackboard.ai.dto.AsyncTaskDTO;
import com.blackboard.ai.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking async AI generation tasks.
 * 
 * <p><b>Architecture Decision:</b> Using in-memory ConcurrentHashMap for simplicity
 * in this demo. In production, this would be replaced with:
 * <ul>
 *   <li>Redis for distributed caching with TTL</li>
 *   <li>Database storage for persistence across restarts</li>
 *   <li>Message queue for reliable task processing</li>
 * </ul>
 * 
 * <p><b>Cleanup:</b> Completed/failed tasks are retained for a configured period
 * for client polling, then automatically cleaned up.
 */
@Service
public class AsyncTaskService {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncTaskService.class);
    
    /** How long to retain completed tasks for polling */
    private static final Duration COMPLETED_TASK_RETENTION = Duration.ofHours(1);
    
    /** How long to retain failed tasks for debugging */
    private static final Duration FAILED_TASK_RETENTION = Duration.ofHours(24);
    
    /** Maximum age for any task (prevents memory leaks) */
    private static final Duration MAX_TASK_AGE = Duration.ofHours(48);
    
    /** In-memory task storage */
    private final ConcurrentHashMap<String, AsyncTaskDTO.TaskState> tasks = new ConcurrentHashMap<>();
    
    /**
     * Create a new task in PENDING state.
     * 
     * @param taskId unique task identifier
     * @param operationType type of operation (e.g., "OUTLINE_GENERATION")
     * @param userId the user who initiated the task
     * @param request the original request parameters
     */
    public void createTask(String taskId, String operationType, Long userId, Map<String, Object> request) {
        log.debug("Creating task {} for user {} type {}", taskId, userId, operationType);
        
        AsyncTaskDTO.TaskState task = new AsyncTaskDTO.TaskState(taskId, operationType, userId, request);
        tasks.put(taskId, task);
    }
    
    /**
     * Get the current status of a task.
     * 
     * @param taskId the task identifier
     * @return task status
     * @throws AiServiceException if task not found
     */
    public AsyncTaskDTO.TaskStatus getTaskStatus(String taskId) {
        AsyncTaskDTO.TaskState task = tasks.get(taskId);
        
        if (task == null) {
            throw new AiServiceException(
                    "TASK_NOT_FOUND",
                    "Task not found: " + taskId,
                    HttpStatus.NOT_FOUND
            );
        }
        
        return task.toTaskStatus();
    }
    
    /**
     * Get task status if the user owns the task.
     * 
     * @param taskId the task identifier
     * @param userId the user ID to verify ownership
     * @return task status
     * @throws AiServiceException if task not found or not owned by user
     */
    public AsyncTaskDTO.TaskStatus getTaskStatus(String taskId, Long userId) {
        AsyncTaskDTO.TaskState task = tasks.get(taskId);
        
        if (task == null) {
            throw new AiServiceException(
                    "TASK_NOT_FOUND",
                    "Task not found: " + taskId,
                    HttpStatus.NOT_FOUND
            );
        }
        
        // Verify ownership
        if (!task.getUserId().equals(userId)) {
            throw new AiServiceException(
                    "TASK_NOT_FOUND",
                    "Task not found: " + taskId,
                    HttpStatus.NOT_FOUND
            );
        }
        
        return task.toTaskStatus();
    }
    
    /**
     * Update task progress.
     * Called periodically during long-running operations.
     * 
     * @param taskId the task identifier
     * @param progress progress percentage (0-100)
     */
    public void updateProgress(String taskId, int progress) {
        AsyncTaskDTO.TaskState task = tasks.get(taskId);
        
        if (task != null) {
            task.markProcessing(progress);
            log.debug("Task {} progress: {}%", taskId, progress);
        }
    }
    
    /**
     * Mark a task as completed with a result.
     * 
     * @param taskId the task identifier
     * @param result the generation result
     */
    public void completeTask(String taskId, Object result) {
        AsyncTaskDTO.TaskState task = tasks.get(taskId);
        
        if (task != null) {
            task.markCompleted(result);
            log.info("Task {} completed successfully", taskId);
        } else {
            log.warn("Attempted to complete unknown task: {}", taskId);
        }
    }
    
    /**
     * Mark a task as failed with an error.
     * 
     * @param taskId the task identifier
     * @param errorCode error code for programmatic handling
     * @param errorMessage human-readable error message
     */
    public void failTask(String taskId, String errorCode, String errorMessage) {
        AsyncTaskDTO.TaskState task = tasks.get(taskId);
        
        if (task != null) {
            task.markFailed(errorCode, errorMessage);
            log.error("Task {} failed: {} - {}", taskId, errorCode, errorMessage);
        } else {
            log.warn("Attempted to fail unknown task: {}", taskId);
        }
    }
    
    /**
     * Check if a task exists (regardless of state).
     * 
     * @param taskId the task identifier
     * @return true if task exists
     */
    public boolean taskExists(String taskId) {
        return tasks.containsKey(taskId);
    }
    
    /**
     * Scheduled cleanup of old tasks.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void cleanupOldTasks() {
        Instant now = Instant.now();
        int removed = 0;
        
        var iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            AsyncTaskDTO.TaskState task = entry.getValue();
            
            boolean shouldRemove = false;
            
            // Always remove tasks older than MAX_TASK_AGE
            if (Duration.between(task.getCreatedAt(), now).compareTo(MAX_TASK_AGE) > 0) {
                shouldRemove = true;
            }
            // Remove completed tasks after retention period
            else if (task.getCompletedAt() != null) {
                Duration retention = task.getStatus() == 
                        com.blackboard.ai.dto.AiGenerationResponse.TaskStatus.FAILED 
                        ? FAILED_TASK_RETENTION 
                        : COMPLETED_TASK_RETENTION;
                
                if (Duration.between(task.getCompletedAt(), now).compareTo(retention) > 0) {
                    shouldRemove = true;
                }
            }
            
            if (shouldRemove) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} old tasks, {} tasks remaining", removed, tasks.size());
        }
    }
    
    /**
     * Get count of active (non-completed) tasks.
     * Used for monitoring.
     * 
     * @return count of active tasks
     */
    public int getActiveTaskCount() {
        return (int) tasks.values().stream()
                .filter(t -> t.getCompletedAt() == null)
                .count();
    }
    
    /**
     * Get total task count.
     * Used for monitoring.
     * 
     * @return total count of tasks in memory
     */
    public int getTotalTaskCount() {
        return tasks.size();
    }
}
