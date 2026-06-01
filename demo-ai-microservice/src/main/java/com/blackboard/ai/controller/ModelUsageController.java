package com.blackboard.ai.controller;

import com.blackboard.ai.dto.ModelUsageDTO;
import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.exception.GlobalExceptionHandler.ErrorResponse;
import com.blackboard.ai.service.ModelUsageService;
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
 * REST controller for AI model usage tracking and quota management.
 * 
 * <p><b>API Path Pattern:</b> {@code /v1/ai-playground/models/usage}
 * 
 * <p><b>Architecture Decision:</b> Usage is tracked per user, per model, per day.
 * This endpoint provides transparency into quota consumption and remaining allowance.
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/ai-playground/models")
@Tag(name = "Model Usage", description = "Track AI model usage and quotas")
@SecurityRequirement(name = "bearerAuth")
public class ModelUsageController {
    
    private static final Logger log = LoggerFactory.getLogger(ModelUsageController.class);
    
    private final ModelUsageService modelUsageService;
    
    public ModelUsageController(ModelUsageService modelUsageService) {
        this.modelUsageService = modelUsageService;
    }
    
    /**
     * Get usage statistics for all AI models.
     * Returns current usage and remaining quota for each model.
     * 
     * @param jwt the authenticated user's JWT token
     * @return usage statistics for all models
     */
    @GetMapping("/usage")
    @Operation(
            summary = "Get model usage",
            description = "Returns current usage statistics and remaining quota for all AI models. " +
                    "Quotas reset daily at midnight UTC."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usage retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ModelUsageDTO.UsageResponse> getUsage(
            @AuthenticationPrincipal Jwt jwt) {
        
        Long userId = extractUserId(jwt);
        log.debug("Getting usage for user {}", userId);
        
        ModelUsageDTO.UsageResponse response = modelUsageService.getUsage(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check quota for a specific model.
     * Use this to verify quota availability before initiating a generation request.
     * 
     * @param jwt the authenticated user's JWT token
     * @param model the AI model to check
     * @return quota check result
     */
    @GetMapping("/usage/{model}/check")
    @Operation(
            summary = "Check model quota",
            description = "Checks if the user has remaining quota for a specific AI model. " +
                    "Use this before initiating a generation request to avoid quota errors."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quota check completed"),
            @ApiResponse(responseCode = "400", description = "Invalid model specified",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ModelUsageDTO.QuotaCheckResponse> checkQuota(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "AI model to check quota for", required = true)
            @PathVariable AiModel model) {
        
        Long userId = extractUserId(jwt);
        log.debug("Checking quota for user {} model {}", userId, model);
        
        ModelUsageDTO.QuotaCheckResponse response = modelUsageService.checkQuota(userId, model);
        return ResponseEntity.ok(response);
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
