package com.blackboard.ai.dto;

import com.blackboard.ai.entity.ModelUsage;
import com.blackboard.ai.entity.enums.AiModel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data Transfer Objects for model usage tracking and quota information.
 * 
 * <p><b>Architecture Decision:</b> Using records for immutable DTOs.
 * The quota information includes both current usage and limits for
 * transparent quota management on the client side.
 */
public class ModelUsageDTO {
    
    /**
     * Usage information for a single model.
     */
    @Schema(description = "Usage information for a specific AI model")
    public record ModelInfo(
            @Schema(description = "Model identifier")
            AiModel model,
            
            @Schema(description = "Human-readable model name", example = "Amazon Nova Micro")
            String displayName,
            
            @Schema(description = "Model description")
            String description,
            
            @Schema(description = "Number of requests used today", example = "5")
            int usedToday,
            
            @Schema(description = "Daily quota limit", example = "20")
            int dailyQuota,
            
            @Schema(description = "Remaining requests available", example = "15")
            int remaining,
            
            @Schema(description = "Whether quota is exhausted")
            boolean quotaExceeded
    ) {
        /**
         * Create from entity with quota calculation.
         */
        public static ModelInfo fromUsage(AiModel model, int usedToday) {
            int remaining = model.getRemainingQuota(usedToday);
            return new ModelInfo(
                    model,
                    model.getDisplayName(),
                    model.getDescription(),
                    usedToday,
                    model.getDailyQuota(),
                    remaining,
                    remaining == 0
            );
        }
        
        /**
         * Create for a model with no usage today.
         */
        public static ModelInfo forUnusedModel(AiModel model) {
            return fromUsage(model, 0);
        }
    }
    
    /**
     * Complete usage response including all models.
     */
    @Schema(description = "Complete model usage information for all available models")
    public record UsageResponse(
            @Schema(description = "Date for this usage data")
            LocalDate date,
            
            @Schema(description = "Total requests made across all models today")
            int totalUsedToday,
            
            @Schema(description = "Usage details per model")
            List<ModelInfo> models
    ) {
        /**
         * Create usage response from a list of usage records.
         * Includes all available models, even those with no usage.
         */
        public static UsageResponse fromUsageRecords(List<ModelUsage> records, LocalDate date) {
            // Map existing usage records by model
            Map<AiModel, Integer> usageMap = records.stream()
                    .collect(Collectors.toMap(
                            ModelUsage::getModelName,
                            ModelUsage::getUsageCount
                    ));
            
            // Create ModelInfo for ALL models (including unused ones)
            List<ModelInfo> modelInfos = Arrays.stream(AiModel.values())
                    .map(model -> {
                        int used = usageMap.getOrDefault(model, 0);
                        return ModelInfo.fromUsage(model, used);
                    })
                    .collect(Collectors.toList());
            
            // Calculate total usage
            int totalUsed = modelInfos.stream()
                    .mapToInt(ModelInfo::usedToday)
                    .sum();
            
            return new UsageResponse(date, totalUsed, modelInfos);
        }
    }
    
    /**
     * Simplified quota check response.
     * Used for quick quota validation before processing requests.
     */
    @Schema(description = "Quick quota check result")
    public record QuotaCheckResponse(
            @Schema(description = "Model being checked")
            AiModel model,
            
            @Schema(description = "Whether the user can make a request with this model")
            boolean allowed,
            
            @Schema(description = "Remaining requests if allowed, 0 if not")
            int remaining,
            
            @Schema(description = "Message explaining the result")
            String message
    ) {
        public static QuotaCheckResponse allowed(AiModel model, int remaining) {
            return new QuotaCheckResponse(
                    model, 
                    true, 
                    remaining,
                    String.format("You have %d %s request(s) remaining today", 
                            remaining, model.getDisplayName())
            );
        }
        
        public static QuotaCheckResponse denied(AiModel model) {
            return new QuotaCheckResponse(
                    model, 
                    false, 
                    0,
                    String.format("Daily quota exceeded for %s. Quota resets at midnight UTC.", 
                            model.getDisplayName())
            );
        }
    }
}
