package com.blackboard.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * DTOs for async task status tracking.
 * 
 * <p><b>Architecture Decision:</b> Tasks are tracked in-memory using ConcurrentHashMap
 * for simplicity in this demo. In production, this would use Redis or a database
 * for persistence across restarts and horizontal scaling.
 */
public class AsyncTaskDTO {
    
    /**
     * Complete task status response.
     */
    @Schema(description = "Async task status details")
    public record TaskStatus(
            @Schema(description = "Unique task identifier")
            String taskId,
            
            @Schema(description = "Current task status")
            AiGenerationResponse.TaskStatus status,
            
            @Schema(description = "Progress percentage (0-100)")
            Integer progress,
            
            @Schema(description = "When the task was created")
            Instant createdAt,
            
            @Schema(description = "When the task started processing")
            Instant startedAt,
            
            @Schema(description = "When the task completed (success or failure)")
            Instant completedAt,
            
            @Schema(description = "Task result if completed successfully")
            Object result,
            
            @Schema(description = "Error details if task failed")
            ErrorInfo error,
            
            @Schema(description = "Additional task metadata")
            Map<String, Object> metadata
    ) {
        /**
         * Create a pending task status.
         */
        public static TaskStatus pending(String taskId, Map<String, Object> metadata) {
            return new TaskStatus(
                    taskId,
                    AiGenerationResponse.TaskStatus.PENDING,
                    0,
                    Instant.now(),
                    null,
                    null,
                    null,
                    null,
                    metadata
            );
        }
        
        /**
         * Create a processing task status.
         */
        public static TaskStatus processing(String taskId, int progress, Instant createdAt, 
                                            Map<String, Object> metadata) {
            return new TaskStatus(
                    taskId,
                    AiGenerationResponse.TaskStatus.PROCESSING,
                    progress,
                    createdAt,
                    Instant.now(),
                    null,
                    null,
                    null,
                    metadata
            );
        }
        
        /**
         * Create a completed task status.
         */
        public static TaskStatus completed(String taskId, Object result, Instant createdAt,
                                           Instant startedAt, Map<String, Object> metadata) {
            return new TaskStatus(
                    taskId,
                    AiGenerationResponse.TaskStatus.COMPLETED,
                    100,
                    createdAt,
                    startedAt,
                    Instant.now(),
                    result,
                    null,
                    metadata
            );
        }
        
        /**
         * Create a failed task status.
         */
        public static TaskStatus failed(String taskId, String errorCode, String errorMessage,
                                        Instant createdAt, Instant startedAt, 
                                        Map<String, Object> metadata) {
            return new TaskStatus(
                    taskId,
                    AiGenerationResponse.TaskStatus.FAILED,
                    null,
                    createdAt,
                    startedAt,
                    Instant.now(),
                    null,
                    new ErrorInfo(errorCode, errorMessage),
                    metadata
            );
        }
    }
    
    /**
     * Error information for failed tasks.
     */
    @Schema(description = "Error details for a failed task")
    public record ErrorInfo(
            @Schema(description = "Error code for programmatic handling", example = "AI_TIMEOUT")
            String code,
            
            @Schema(description = "Human-readable error message")
            String message
    ) {}
    
    /**
     * Internal task state for in-memory storage.
     * Not exposed in API responses directly.
     */
    public static class TaskState {
        private final String taskId;
        private final String operationType;
        private final Long userId;
        private final Map<String, Object> request;
        private final Instant createdAt;
        private volatile AiGenerationResponse.TaskStatus status;
        private volatile int progress;
        private volatile Instant startedAt;
        private volatile Instant completedAt;
        private volatile Object result;
        private volatile String errorCode;
        private volatile String errorMessage;
        
        public TaskState(String taskId, String operationType, Long userId, Map<String, Object> request) {
            this.taskId = taskId;
            this.operationType = operationType;
            this.userId = userId;
            this.request = request;
            this.createdAt = Instant.now();
            this.status = AiGenerationResponse.TaskStatus.PENDING;
            this.progress = 0;
        }
        
        // Getters
        public String getTaskId() { return taskId; }
        public String getOperationType() { return operationType; }
        public Long getUserId() { return userId; }
        public Map<String, Object> getRequest() { return request; }
        public Instant getCreatedAt() { return createdAt; }
        public AiGenerationResponse.TaskStatus getStatus() { return status; }
        public int getProgress() { return progress; }
        public Instant getStartedAt() { return startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public Object getResult() { return result; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        
        // State transitions
        public void markProcessing(int progress) {
            this.status = AiGenerationResponse.TaskStatus.PROCESSING;
            this.progress = progress;
            if (this.startedAt == null) {
                this.startedAt = Instant.now();
            }
        }
        
        public void markCompleted(Object result) {
            this.status = AiGenerationResponse.TaskStatus.COMPLETED;
            this.progress = 100;
            this.completedAt = Instant.now();
            this.result = result;
        }
        
        public void markFailed(String errorCode, String errorMessage) {
            this.status = AiGenerationResponse.TaskStatus.FAILED;
            this.completedAt = Instant.now();
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        
        /**
         * Convert to API response format.
         */
        public TaskStatus toTaskStatus() {
            Map<String, Object> metadata = Map.of(
                    "operationType", operationType
            );
            
            return new TaskStatus(
                    taskId,
                    status,
                    progress,
                    createdAt,
                    startedAt,
                    completedAt,
                    result,
                    errorCode != null ? new ErrorInfo(errorCode, errorMessage) : null,
                    metadata
            );
        }
    }
}
