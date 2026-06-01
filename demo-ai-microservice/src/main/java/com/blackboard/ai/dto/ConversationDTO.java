package com.blackboard.ai.dto;

import com.blackboard.ai.entity.PlaygroundConversation;
import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.entity.enums.ContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Data Transfer Object for AI Playground conversations.
 * 
 * <p><b>Architecture Decision:</b> Using nested records for request/response separation.
 * This provides clear API contracts and allows for different validation rules
 * on create vs. update operations.
 */
public class ConversationDTO {
    
    /**
     * Response DTO for conversation data.
     * Used in GET responses and as part of list responses.
     */
    @Schema(description = "AI Playground conversation details")
    public record Response(
            @Schema(description = "Unique conversation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
            String id,
            
            @Schema(description = "User-provided title", example = "Help with Python assignment")
            String title,
            
            @Schema(description = "Context type for AI responses")
            ContextType contextType,
            
            @Schema(description = "AI model used for this conversation")
            AiModel modelName,
            
            @Schema(description = "When the conversation was created")
            Instant createdAt,
            
            @Schema(description = "When the conversation was last modified")
            Instant lastModifiedAt
    ) {
        /**
         * Factory method to create Response from entity.
         */
        public static Response fromEntity(PlaygroundConversation entity) {
            return new Response(
                    entity.getConversationUid(),
                    entity.getTitle(),
                    entity.getContextType(),
                    entity.getModelName(),
                    entity.getCreatedAt(),
                    entity.getLastModifiedAt()
            );
        }
    }
    
    /**
     * Request DTO for creating a new conversation.
     */
    @Schema(description = "Request to create a new AI Playground conversation")
    public record CreateRequest(
            @Schema(description = "Title for the conversation", example = "Chemistry Study Session")
            @NotBlank(message = "Title is required")
            @Size(max = 255, message = "Title must be 255 characters or less")
            String title,
            
            @Schema(description = "Context type determines how AI interprets queries")
            @NotNull(message = "Context type is required")
            ContextType contextType,
            
            @Schema(description = "AI model to use for this conversation")
            @NotNull(message = "Model name is required")
            AiModel modelName
    ) {
        /**
         * Convert to entity for persistence.
         */
        public PlaygroundConversation toEntity(Long usersPk1) {
            return new PlaygroundConversation(usersPk1, title, contextType, modelName);
        }
    }
    
    /**
     * Request DTO for updating an existing conversation.
     * All fields are optional - only provided fields are updated.
     */
    @Schema(description = "Request to update an AI Playground conversation (PATCH semantics)")
    public record UpdateRequest(
            @Schema(description = "New title for the conversation")
            @Size(max = 255, message = "Title must be 255 characters or less")
            String title,
            
            @Schema(description = "New context type")
            ContextType contextType,
            
            @Schema(description = "New AI model")
            AiModel modelName
    ) {
        /**
         * Apply updates to an existing entity.
         * Only non-null fields are applied (PATCH semantics).
         */
        public void applyTo(PlaygroundConversation entity) {
            if (title != null && !title.isBlank()) {
                entity.setTitle(title);
            }
            if (contextType != null) {
                entity.setContextType(contextType);
            }
            if (modelName != null) {
                entity.setModelName(modelName);
            }
        }
    }
    
    /**
     * Paginated list response for conversations.
     */
    @Schema(description = "Paginated list of conversations")
    public record ListResponse(
            @Schema(description = "List of conversations on this page")
            java.util.List<Response> content,
            
            @Schema(description = "Current page number (0-indexed)")
            int page,
            
            @Schema(description = "Number of items per page")
            int size,
            
            @Schema(description = "Total number of conversations")
            long totalElements,
            
            @Schema(description = "Total number of pages")
            int totalPages,
            
            @Schema(description = "Whether this is the first page")
            boolean first,
            
            @Schema(description = "Whether this is the last page")
            boolean last
    ) {}
}
