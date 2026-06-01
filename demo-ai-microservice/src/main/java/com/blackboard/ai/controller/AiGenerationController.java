package com.blackboard.ai.controller;

import com.blackboard.ai.dto.AiGenerationRequest;
import com.blackboard.ai.dto.AiGenerationResponse;
import com.blackboard.ai.exception.GlobalExceptionHandler.ErrorResponse;
import com.blackboard.ai.service.AiGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI content generation operations.
 * 
 * <p><b>API Path Pattern:</b> {@code /v1/courses/{courseId}/ai/...}
 * 
 * <p><b>Architecture Decision:</b> All generation operations are asynchronous
 * to handle the variable latency of AI model responses. The client receives
 * a task ID immediately and polls the task status endpoint for completion.
 * 
 * <p><b>Quota Management:</b> Each request checks and reserves quota before
 * submitting to the AI backend. If quota is exceeded, a 429 Too Many Requests
 * error is returned immediately.
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/courses/{courseId}/ai")
@Tag(name = "AI Generation", description = "Generate AI-powered course content")
@SecurityRequirement(name = "bearerAuth")
public class AiGenerationController {
    
    private static final Logger log = LoggerFactory.getLogger(AiGenerationController.class);
    
    private final AiGenerationService aiGenerationService;
    
    public AiGenerationController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }
    
    /**
     * Generate a course outline using AI.
     * 
     * @param jwt the authenticated user's JWT token
     * @param courseId the course ID
     * @param request the outline generation request
     * @return async response with task ID for polling
     */
    @PostMapping("/outline")
    @Operation(
            summary = "Generate course outline",
            description = "Uses AI to generate a structured course outline based on the provided " +
                    "course title and parameters. Returns a task ID for async status polling."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Generation task accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Quota exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiGenerationResponse.AsyncResponse> generateOutline(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Course ID", required = true)
            @PathVariable String courseId,
            @Valid @RequestBody AiGenerationRequest.OutlineRequest request) {
        
        Long userId = extractUserId(jwt);
        String tenantId = extractTenantId(jwt);
        
        log.info("Generating outline for course {} by user {}", courseId, userId);
        
        AiGenerationResponse.AsyncResponse response = aiGenerationService.generateCourseOutline(
                userId, tenantId, courseId, request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Generate content suggestions for a content item.
     * 
     * @param jwt the authenticated user's JWT token
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the content suggestion request
     * @return async response with task ID for polling
     */
    @PostMapping("/contents/{contentId}/suggest")
    @Operation(
            summary = "Generate content suggestions",
            description = "Uses AI to suggest content improvements or additions for an existing " +
                    "content item. Returns a task ID for async status polling."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Generation task accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Quota exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiGenerationResponse.AsyncResponse> generateContentSuggestion(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Course ID", required = true)
            @PathVariable String courseId,
            @Parameter(description = "Content item ID", required = true)
            @PathVariable String contentId,
            @Valid @RequestBody AiGenerationRequest.ContentSuggestionRequest request) {
        
        Long userId = extractUserId(jwt);
        String tenantId = extractTenantId(jwt);
        
        log.info("Generating content suggestions for content {} in course {} by user {}", 
                contentId, courseId, userId);
        
        AiGenerationResponse.AsyncResponse response = aiGenerationService.generateContentSuggestion(
                userId, tenantId, courseId, contentId, request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Generate images for a content item.
     * 
     * @param jwt the authenticated user's JWT token
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the image generation request
     * @return async response with task ID for polling
     */
    @PostMapping("/contents/{contentId}/images")
    @Operation(
            summary = "Generate images",
            description = "Uses AI to generate educational images based on a text prompt. " +
                    "Returns a task ID for async status polling."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Generation task accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Quota exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiGenerationResponse.AsyncResponse> generateImages(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Course ID", required = true)
            @PathVariable String courseId,
            @Parameter(description = "Content item ID", required = true)
            @PathVariable String contentId,
            @Valid @RequestBody AiGenerationRequest.ImageGenerationRequest request) {
        
        Long userId = extractUserId(jwt);
        String tenantId = extractTenantId(jwt);
        
        log.info("Generating images for content {} in course {} by user {}", 
                contentId, courseId, userId);
        
        AiGenerationResponse.AsyncResponse response = aiGenerationService.generateImages(
                userId, tenantId, courseId, contentId, request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Generate flashcards for a learning activity.
     * 
     * @param jwt the authenticated user's JWT token
     * @param courseId the course ID
     * @param request the flashcard generation request
     * @return async response with task ID for polling
     */
    @PostMapping("/learning-activities/ai/flashcards")
    @Operation(
            summary = "Generate flashcards",
            description = "Uses AI to generate study flashcards based on a topic or content. " +
                    "Returns a task ID for async status polling."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Generation task accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Quota exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AiGenerationResponse.AsyncResponse> generateFlashcards(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Course ID", required = true)
            @PathVariable String courseId,
            @Valid @RequestBody AiGenerationRequest.FlashcardRequest request) {
        
        Long userId = extractUserId(jwt);
        String tenantId = extractTenantId(jwt);
        
        log.info("Generating flashcards for course {} by user {}", courseId, userId);
        
        AiGenerationResponse.AsyncResponse response = aiGenerationService.generateFlashcards(
                userId, tenantId, courseId, request);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
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
    
    /**
     * Extract tenant ID from JWT token.
     * Falls back to a default for demo purposes.
     */
    private String extractTenantId(Jwt jwt) {
        Object tenantId = jwt.getClaim("tenant_id");
        if (tenantId != null) {
            return tenantId.toString();
        }
        // Demo fallback
        return "demo-tenant";
    }
}
