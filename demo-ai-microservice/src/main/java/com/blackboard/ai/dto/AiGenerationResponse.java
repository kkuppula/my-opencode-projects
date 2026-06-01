package com.blackboard.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTOs for AI generation operations.
 */
public class AiGenerationResponse {
    
    /**
     * Synchronous generation response with immediate results.
     */
    @Schema(description = "Response from synchronous AI generation")
    public record ImmediateResponse(
            @Schema(description = "Whether generation was successful")
            boolean success,
            
            @Schema(description = "Generated content")
            Object content,
            
            @Schema(description = "Model used for generation")
            String model,
            
            @Schema(description = "Generation timestamp")
            Instant generatedAt,
            
            @Schema(description = "Additional metadata about the generation")
            Map<String, Object> metadata
    ) {
        public static ImmediateResponse success(Object content, String model) {
            return new ImmediateResponse(
                    true,
                    content,
                    model,
                    Instant.now(),
                    Map.of()
            );
        }
        
        public static ImmediateResponse error(String message) {
            return new ImmediateResponse(
                    false,
                    Map.of("error", message),
                    null,
                    Instant.now(),
                    Map.of()
            );
        }
    }
    
    /**
     * Async generation response with task ID for polling.
     */
    @Schema(description = "Response from async AI generation (task initiated)")
    public record AsyncResponse(
            @Schema(description = "Task ID for status polling")
            String taskId,
            
            @Schema(description = "Current task status")
            TaskStatus status,
            
            @Schema(description = "Estimated completion time in seconds")
            Integer estimatedSeconds,
            
            @Schema(description = "URL to poll for status", example = "/v1/ai/tasks/abc123")
            String statusUrl,
            
            @Schema(description = "Message about the task")
            String message
    ) {
        public static AsyncResponse created(String taskId) {
            return new AsyncResponse(
                    taskId,
                    TaskStatus.PENDING,
                    30,
                    "/v1/ai/tasks/" + taskId,
                    "Task created successfully. Poll the status URL for updates."
            );
        }
    }
    
    /**
     * Task status enumeration.
     */
    @Schema(description = "Status of an async AI generation task")
    public enum TaskStatus {
        @Schema(description = "Task is queued but not yet started")
        PENDING,
        
        @Schema(description = "Task is currently being processed")
        PROCESSING,
        
        @Schema(description = "Task completed successfully")
        COMPLETED,
        
        @Schema(description = "Task failed")
        FAILED,
        
        @Schema(description = "Task was cancelled")
        CANCELLED
    }
    
    /**
     * Course outline response.
     */
    @Schema(description = "Generated course outline")
    public record OutlineResponse(
            @Schema(description = "Course title")
            String courseTitle,
            
            @Schema(description = "Course modules/units")
            List<Module> modules,
            
            @Schema(description = "Generation metadata")
            Map<String, Object> metadata
    ) {
        @Schema(description = "Course module")
        public record Module(
                @Schema(description = "Module number")
                int number,
                
                @Schema(description = "Module title")
                String title,
                
                @Schema(description = "Module description")
                String description,
                
                @Schema(description = "Learning objectives for this module")
                List<String> objectives,
                
                @Schema(description = "Topics covered in this module")
                List<String> topics,
                
                @Schema(description = "Estimated duration in hours")
                Integer estimatedHours
        ) {}
    }
    
    /**
     * Content suggestion response.
     */
    @Schema(description = "Generated content suggestions")
    public record ContentSuggestionResponse(
            @Schema(description = "Type of content generated")
            String contentType,
            
            @Schema(description = "Generated suggestions")
            List<Suggestion> suggestions
    ) {
        @Schema(description = "Individual content suggestion")
        public record Suggestion(
                @Schema(description = "Suggestion content")
                String content,
                
                @Schema(description = "Confidence score (0-1)")
                Double confidence,
                
                @Schema(description = "Tags or categories")
                List<String> tags
        ) {}
    }
    
    /**
     * Flashcard generation response.
     */
    @Schema(description = "Generated flashcards")
    public record FlashcardResponse(
            @Schema(description = "Topic the flashcards cover")
            String topic,
            
            @Schema(description = "Generated flashcards")
            List<Flashcard> flashcards,
            
            @Schema(description = "Total count")
            int count
    ) {
        @Schema(description = "Individual flashcard")
        public record Flashcard(
                @Schema(description = "Question/front of card")
                String question,
                
                @Schema(description = "Answer/back of card")
                String answer,
                
                @Schema(description = "Difficulty level")
                String difficulty,
                
                @Schema(description = "Hint for the question")
                String hint
        ) {}
    }
    
    /**
     * Image generation response.
     */
    @Schema(description = "Generated images")
    public record ImageResponse(
            @Schema(description = "Original prompt")
            String prompt,
            
            @Schema(description = "Generated images")
            List<GeneratedImage> images
    ) {
        @Schema(description = "Generated image details")
        public record GeneratedImage(
                @Schema(description = "Image URL")
                String url,
                
                @Schema(description = "Image width in pixels")
                int width,
                
                @Schema(description = "Image height in pixels")
                int height,
                
                @Schema(description = "Image format")
                String format
        ) {}
    }
}
