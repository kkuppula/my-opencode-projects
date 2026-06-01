package com.blackboard.ai.controller;

import com.blackboard.ai.dto.AiSettingsDTO;
import com.blackboard.ai.exception.GlobalExceptionHandler.ErrorResponse;
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
 * REST controller for AI feature settings and configuration.
 * 
 * <p><b>API Path Patterns:</b>
 * <ul>
 *   <li>{@code GET /v1/ai/settings} - System-wide settings</li>
 *   <li>{@code GET /v1/courses/{courseId}/ai/settings} - Course-specific settings</li>
 * </ul>
 * 
 * <p><b>Architecture Decision:</b> Settings are currently returned as static
 * configuration. In production, these would be fetched from a configuration
 * service to support feature flags per tenant/course.
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@RestController
@Tag(name = "AI Settings", description = "Get AI feature settings and configuration")
@SecurityRequirement(name = "bearerAuth")
public class AiSettingsController {
    
    private static final Logger log = LoggerFactory.getLogger(AiSettingsController.class);
    
    /**
     * Get system-wide AI settings.
     * 
     * @param jwt the authenticated user's JWT token
     * @return system AI settings
     */
    @GetMapping("/v1/ai/settings")
    @Operation(
            summary = "Get system AI settings",
            description = "Returns system-wide AI feature settings including available models, " +
                    "quota information, and feature flags"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiSettingsDTO.SystemSettings> getSystemSettings(
            @AuthenticationPrincipal Jwt jwt) {
        
        log.debug("Getting system AI settings");
        
        // In production, this would fetch from a configuration service
        // For demo, return default enabled settings
        AiSettingsDTO.SystemSettings settings = AiSettingsDTO.SystemSettings.defaultSettings();
        
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Get course-specific AI settings.
     * 
     * @param jwt the authenticated user's JWT token
     * @param courseId the course ID
     * @return course AI settings
     */
    @GetMapping("/v1/courses/{courseId}/ai/settings")
    @Operation(
            summary = "Get course AI settings",
            description = "Returns AI feature settings specific to a course, including which " +
                    "AI features are enabled and any course-specific configuration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Course not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiSettingsDTO.CourseSettings> getCourseSettings(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Course ID", required = true)
            @PathVariable String courseId) {
        
        log.debug("Getting AI settings for course {}", courseId);
        
        // In production, this would:
        // 1. Verify the user has access to the course
        // 2. Fetch course/institution-specific settings from config service
        // For demo, return default enabled settings
        AiSettingsDTO.CourseSettings settings = AiSettingsDTO.CourseSettings.defaultEnabled(courseId);
        
        return ResponseEntity.ok(settings);
    }
}
