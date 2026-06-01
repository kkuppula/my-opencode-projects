package com.blackboard.ai.dto;

import com.blackboard.ai.entity.enums.AiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTOs for AI generation operations.
 * 
 * <p><b>Architecture Decision:</b> Using a single flexible request format that
 * matches the ai-integrations service contract. The context and parameters
 * maps allow for operation-specific data without requiring separate DTOs.
 */
public class AiGenerationRequest {
    
    /**
     * Base request for AI generation operations.
     * Used for outline generation, content suggestions, images, and flashcards.
     */
    @Schema(description = "Request for AI content generation")
    public record GenerationRequest(
            @Schema(description = "AI model to use for generation")
            AiModel model,
            
            @Schema(description = "Context information for the generation", 
                    example = "{\"courseTitle\": \"Introduction to Biology\", \"topic\": \"Cell Division\"}")
            @NotNull(message = "Context is required")
            Map<String, Object> context,
            
            @Schema(description = "Generation parameters",
                    example = "{\"maxItems\": 10, \"style\": \"academic\"}")
            Map<String, Object> parameters
    ) {
        /**
         * Get model or default to AMAZON_NOVA_MICRO.
         */
        public AiModel getModelOrDefault() {
            return model != null ? model : AiModel.AMAZON_NOVA_MICRO;
        }
    }
    
    /**
     * Request for course outline generation.
     */
    @Schema(description = "Request to generate a course outline")
    public record OutlineRequest(
            @Schema(description = "AI model to use")
            AiModel model,
            
            @Schema(description = "Course title", example = "Introduction to Machine Learning")
            @NotBlank(message = "Course title is required")
            String courseTitle,
            
            @Schema(description = "Course description or objectives")
            String description,
            
            @Schema(description = "Target audience level", example = "undergraduate")
            String level,
            
            @Schema(description = "Desired number of modules/units", example = "8")
            Integer numberOfModules,
            
            @Schema(description = "Additional context or requirements")
            String additionalContext
    ) {
        /**
         * Convert to generic generation request format.
         */
        public GenerationRequest toGenerationRequest() {
            Map<String, Object> context = new java.util.HashMap<>();
            context.put("courseTitle", courseTitle);
            if (description != null) context.put("description", description);
            if (level != null) context.put("level", level);
            if (additionalContext != null) context.put("additionalContext", additionalContext);
            
            Map<String, Object> params = new java.util.HashMap<>();
            if (numberOfModules != null) params.put("numberOfModules", numberOfModules);
            
            return new GenerationRequest(
                    model != null ? model : AiModel.AMAZON_NOVA_MICRO,
                    context,
                    params
            );
        }
    }
    
    /**
     * Request for content suggestions.
     */
    @Schema(description = "Request to generate content suggestions")
    public record ContentSuggestionRequest(
            @Schema(description = "AI model to use")
            AiModel model,
            
            @Schema(description = "Type of content to generate", example = "quiz_question")
            @NotBlank(message = "Content type is required")
            String contentType,
            
            @Schema(description = "Topic or subject matter")
            @NotBlank(message = "Topic is required")
            String topic,
            
            @Schema(description = "Existing content to enhance or expand upon")
            String existingContent,
            
            @Schema(description = "Number of suggestions to generate", example = "5")
            Integer count
    ) {
        public GenerationRequest toGenerationRequest() {
            Map<String, Object> context = Map.of(
                    "contentType", contentType,
                    "topic", topic,
                    "existingContent", existingContent != null ? existingContent : ""
            );
            
            Map<String, Object> params = new java.util.HashMap<>();
            if (count != null) params.put("count", count);
            
            return new GenerationRequest(
                    model != null ? model : AiModel.AMAZON_NOVA_MICRO,
                    context,
                    params
            );
        }
    }
    
    /**
     * Request for image generation.
     */
    @Schema(description = "Request to generate images")
    public record ImageGenerationRequest(
            @Schema(description = "AI model to use")
            AiModel model,
            
            @Schema(description = "Image description/prompt")
            @NotBlank(message = "Prompt is required")
            String prompt,
            
            @Schema(description = "Image style", example = "educational_diagram")
            String style,
            
            @Schema(description = "Desired width in pixels", example = "512")
            Integer width,
            
            @Schema(description = "Desired height in pixels", example = "512")
            Integer height,
            
            @Schema(description = "Number of images to generate", example = "1")
            Integer count
    ) {
        public GenerationRequest toGenerationRequest() {
            Map<String, Object> context = Map.of("prompt", prompt);
            
            Map<String, Object> params = new java.util.HashMap<>();
            if (style != null) params.put("style", style);
            if (width != null) params.put("width", width);
            if (height != null) params.put("height", height);
            if (count != null) params.put("count", count);
            
            return new GenerationRequest(
                    model != null ? model : AiModel.AMAZON_NOVA_LITE,
                    context,
                    params
            );
        }
    }
    
    /**
     * Request for flashcard generation.
     */
    @Schema(description = "Request to generate flashcards")
    public record FlashcardRequest(
            @Schema(description = "AI model to use")
            AiModel model,
            
            @Schema(description = "Topic for flashcards")
            @NotBlank(message = "Topic is required")
            String topic,
            
            @Schema(description = "Source content to generate flashcards from")
            String sourceContent,
            
            @Schema(description = "Number of flashcards to generate", example = "10")
            Integer count,
            
            @Schema(description = "Difficulty level", example = "intermediate")
            String difficulty
    ) {
        public GenerationRequest toGenerationRequest() {
            Map<String, Object> context = new java.util.HashMap<>();
            context.put("topic", topic);
            if (sourceContent != null) context.put("sourceContent", sourceContent);
            
            Map<String, Object> params = new java.util.HashMap<>();
            if (count != null) params.put("count", count);
            if (difficulty != null) params.put("difficulty", difficulty);
            
            return new GenerationRequest(
                    model != null ? model : AiModel.AMAZON_NOVA_MICRO,
                    context,
                    params
            );
        }
    }
}
