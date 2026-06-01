package com.blackboard.ai.controller;

import com.blackboard.ai.dto.ConversationDTO;
import com.blackboard.ai.exception.GlobalExceptionHandler.ErrorResponse;
import com.blackboard.ai.service.ConversationService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI Playground conversations.
 * 
 * <p><b>API Path Pattern:</b> {@code /v1/ai-playground/conversations}
 * 
 * <p><b>Architecture Decision:</b> Conversations are user-scoped. The authenticated
 * user's ID is extracted from the JWT token and used to scope all operations.
 * Users cannot access other users' conversations.
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/ai-playground/conversations")
@Tag(name = "AI Playground Conversations", description = "Manage AI playground conversation sessions")
@SecurityRequirement(name = "bearerAuth")
public class PlaygroundConversationController {
    
    private static final Logger log = LoggerFactory.getLogger(PlaygroundConversationController.class);
    
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    private final ConversationService conversationService;
    
    public PlaygroundConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    /**
     * Create a new AI Playground conversation.
     * 
     * @param jwt the authenticated user's JWT token
     * @param request the conversation creation request
     * @return the created conversation
     */
    @PostMapping
    @Operation(
            summary = "Create a new conversation",
            description = "Creates a new AI Playground conversation for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conversation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationDTO.Response> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConversationDTO.CreateRequest request) {
        
        Long userId = extractUserId(jwt);
        log.info("Creating conversation for user {}", userId);
        
        ConversationDTO.Response response = conversationService.createConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * List conversations for the authenticated user with pagination.
     * 
     * @param jwt the authenticated user's JWT token
     * @param page page number (0-indexed)
     * @param size page size (max 100)
     * @return paginated list of conversations
     */
    @GetMapping
    @Operation(
            summary = "List conversations",
            description = "Returns a paginated list of the user's AI Playground conversations, " +
                    "ordered by last modified date (most recent first)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationDTO.ListResponse> listConversations(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = extractUserId(jwt);
        
        // Enforce page size limits
        size = Math.min(size, MAX_PAGE_SIZE);
        if (size <= 0) size = DEFAULT_PAGE_SIZE;
        
        Pageable pageable = PageRequest.of(page, size);
        ConversationDTO.ListResponse response = conversationService.listConversations(userId, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a specific conversation by ID.
     * 
     * @param jwt the authenticated user's JWT token
     * @param conversationId the conversation UUID
     * @return the conversation details
     */
    @GetMapping("/{conversationId}")
    @Operation(
            summary = "Get a conversation",
            description = "Retrieves details of a specific conversation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationDTO.Response> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable String conversationId) {
        
        Long userId = extractUserId(jwt);
        ConversationDTO.Response response = conversationService.getConversation(userId, conversationId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update a conversation (PATCH semantics).
     * Only provided fields are updated.
     * 
     * @param jwt the authenticated user's JWT token
     * @param conversationId the conversation UUID
     * @param request the update request (partial)
     * @return the updated conversation
     */
    @PatchMapping("/{conversationId}")
    @Operation(
            summary = "Update a conversation",
            description = "Updates a conversation using PATCH semantics (only provided fields are changed)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationDTO.Response> updateConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationDTO.UpdateRequest request) {
        
        Long userId = extractUserId(jwt);
        log.info("Updating conversation {} for user {}", conversationId, userId);
        
        ConversationDTO.Response response = conversationService.updateConversation(
                userId, conversationId, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a conversation.
     * 
     * @param jwt the authenticated user's JWT token
     * @param conversationId the conversation UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{conversationId}")
    @Operation(
            summary = "Delete a conversation",
            description = "Permanently deletes a conversation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conversation deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable String conversationId) {
        
        Long userId = extractUserId(jwt);
        log.info("Deleting conversation {} for user {}", conversationId, userId);
        
        conversationService.deleteConversation(userId, conversationId);
        
        return ResponseEntity.noContent().build();
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    /**
     * Extract the user ID from the JWT token.
     * 
     * <p><b>Architecture Decision:</b> The user ID is expected in a custom claim
     * "learn_user_id" which is set by the API gateway after validating the
     * Learn session. If not present, falls back to the subject claim.
     */
    private Long extractUserId(Jwt jwt) {
        // Try custom claim first (set by API gateway from Learn session)
        Object learnUserId = jwt.getClaim("learn_user_id");
        if (learnUserId != null) {
            return Long.parseLong(learnUserId.toString());
        }
        
        // Fallback to subject claim (for demo/testing)
        String subject = jwt.getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            // Use hash of subject if not numeric (demo fallback)
            return (long) Math.abs(subject.hashCode());
        }
    }
}
