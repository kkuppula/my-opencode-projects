package com.blackboard.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTOs for AI feature settings/configuration.
 * 
 * <p><b>Architecture Decision:</b> Settings are currently returned as static configuration
 * but designed to support dynamic feature flags from a configuration service in the future.
 */
public class AiSettingsDTO {
    
    /**
     * Course-level AI settings response.
     */
    @Schema(description = "AI feature settings for a specific course")
    public record CourseSettings(
            @Schema(description = "Course ID")
            String courseId,
            
            @Schema(description = "Whether AI features are enabled for this course")
            boolean aiEnabled,
            
            @Schema(description = "Whether course outline generation is available")
            boolean outlineGenerationEnabled,
            
            @Schema(description = "Whether content suggestions are available")
            boolean contentSuggestionsEnabled,
            
            @Schema(description = "Whether image generation is available")
            boolean imageGenerationEnabled,
            
            @Schema(description = "Whether flashcard generation is available")
            boolean flashcardGenerationEnabled,
            
            @Schema(description = "Additional feature-specific settings")
            Map<String, Object> additionalSettings
    ) {
        /**
         * Create default enabled settings for a course.
         * In production, these would come from course/institution configuration.
         */
        public static CourseSettings defaultEnabled(String courseId) {
            return new CourseSettings(
                    courseId,
                    true,
                    true,
                    true,
                    true,
                    true,
                    Map.of(
                            "maxOutlineItems", 20,
                            "maxFlashcardsPerRequest", 25,
                            "supportedImageFormats", java.util.List.of("png", "jpg"),
                            "maxImageWidth", 1024,
                            "maxImageHeight", 1024
                    )
            );
        }
        
        /**
         * Create disabled settings (AI not available for this course).
         */
        public static CourseSettings disabled(String courseId) {
            return new CourseSettings(
                    courseId,
                    false,
                    false,
                    false,
                    false,
                    false,
                    Map.of()
            );
        }
    }
    
    /**
     * System-level AI settings response.
     */
    @Schema(description = "System-wide AI feature settings")
    public record SystemSettings(
            @Schema(description = "Whether AI features are enabled system-wide")
            boolean aiEnabled,
            
            @Schema(description = "Whether the AI Playground feature is available")
            boolean playgroundEnabled,
            
            @Schema(description = "Available AI models")
            java.util.List<ModelInfo> availableModels,
            
            @Schema(description = "System maintenance message, if any")
            String maintenanceMessage,
            
            @Schema(description = "Additional system settings")
            Map<String, Object> additionalSettings
    ) {
        /**
         * Model information for settings response.
         */
        @Schema(description = "Information about an available AI model")
        public record ModelInfo(
                @Schema(description = "Model identifier")
                String id,
                
                @Schema(description = "Display name")
                String displayName,
                
                @Schema(description = "Model description")
                String description,
                
                @Schema(description = "Daily usage quota")
                int dailyQuota,
                
                @Schema(description = "Whether this model is currently available")
                boolean available
        ) {}
        
        /**
         * Create default system settings.
         */
        public static SystemSettings defaultSettings() {
            java.util.List<ModelInfo> models = java.util.Arrays.stream(
                    com.blackboard.ai.entity.enums.AiModel.values())
                    .map(m -> new ModelInfo(
                            m.name(),
                            m.getDisplayName(),
                            m.getDescription(),
                            m.getDailyQuota(),
                            true
                    ))
                    .toList();
            
            return new SystemSettings(
                    true,
                    true,
                    models,
                    null,
                    Map.of(
                            "version", "1.0.0",
                            "quotaResetTime", "00:00 UTC",
                            "supportEmail", "ai-support@blackboard.com"
                    )
            );
        }
    }
}
