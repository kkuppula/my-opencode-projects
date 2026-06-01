package com.blackboard.ai.controller;

import com.blackboard.ai.dto.AsyncTaskDTO;
import com.blackboard.ai.exception.GlobalExceptionHandler.ErrorResponse;
import com.blackboard.ai.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for async task status polling.
 * 
 * <p><b>API Path Pattern:</b> {@code /v1/ai/tasks/{taskId}}
 * 
 * <p><b>Architecture Decision:</b> AI generation operations are asynchronous due to
 * variable AI model latency. Clients poll this endpoint to check task completion.
 * 
 * <p><b>Polling Strategy:</b> Clients should implement exponential backoff:
 * <ul>
 *   <li>First 30 seconds: Poll every 2 seconds</li>
 *   <li>30-60 seconds: Poll every 5 seconds</li>
 *   <li>After 60 seconds: Poll every 10 seconds</li>
 * </ul>
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/ai/tasks")
@Tag(name = "Async Tasks", description = "Poll async AI generation task status")
@SecurityRequirement(name = "bearerAuth")
public class AsyncTaskController {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncTaskController.class);
    
    private final AsyncTaskService asyncTaskService;
    
    public AsyncTaskController(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }
    
    /**
     * Get the status of an async task.
     * 
     * @param jwt the authenticated user's JWT token
     * @param taskId the task ID returned from a generation request
     * @return task status including completion state and result
     */
    @GetMapping("/{taskId}")
    @Operation(
            summary = "Get task status",
            description = "Returns the current status of an async AI generation task. " +
                    "When status is COMPLETED, the result field contains the generation output. " +
                    "When status is FAILED, the error field contains error details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AsyncTaskDTO.TaskStatus> getTaskStatus(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Task ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String taskId) {
        
        Long userId = extractUserId(jwt);
        log.debug("Getting status for task {} by user {}", taskId, userId);
        
        // Verify task ownership
        AsyncTaskDTO.TaskStatus status = asyncTaskService.getTaskStatus(taskId, userId);
        
        return ResponseEntity.ok(status);
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    private Long extractUserId(Jwt jwt) {
        Object learnUserId = jwt.getClaim("learn_user_id");
        if (learnUserId != null) {
            return Long.parseLong(learnUserId.toString());
        }
        
        String subject = jwt.getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            return (long) Math.abs(subject.hashCode());
        }
    }
}
