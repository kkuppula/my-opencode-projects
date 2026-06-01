package com.blackboard.ai.service;

import com.blackboard.ai.client.AiIntegrationsClient;
import com.blackboard.ai.dto.AiGenerationRequest;
import com.blackboard.ai.dto.AiGenerationResponse;
import com.blackboard.ai.entity.enums.AiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for AI content generation operations.
 * 
 * <p><b>Architecture Decision:</b> This service orchestrates AI generation by:
 * <ol>
 *   <li>Checking user quotas via ModelUsageService</li>
 *   <li>Delegating to ai-integrations service via AiIntegrationsClient</li>
 *   <li>Tracking async tasks via AsyncTaskService</li>
 *   <li>Recording usage after successful generation</li>
 * </ol>
 * 
 * <p>Generation operations can be synchronous (immediate response) or
 * asynchronous (returns task ID for polling).
 */
@Service
public class AiGenerationService {
    
    private static final Logger log = LoggerFactory.getLogger(AiGenerationService.class);
    
    private final ModelUsageService modelUsageService;
    private final AsyncTaskService asyncTaskService;
    private final AiIntegrationsClient aiIntegrationsClient;
    
    public AiGenerationService(ModelUsageService modelUsageService,
                               AsyncTaskService asyncTaskService,
                               AiIntegrationsClient aiIntegrationsClient) {
        this.modelUsageService = modelUsageService;
        this.asyncTaskService = asyncTaskService;
        this.aiIntegrationsClient = aiIntegrationsClient;
    }
    
    /**
     * Generate a course outline.
     * This is typically an async operation due to generation time.
     * 
     * @param userId the Learn B2 user ID
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param request the outline generation request
     * @return async response with task ID
     */
    public AiGenerationResponse.AsyncResponse generateCourseOutline(
            Long userId, String tenantId, String courseId,
            AiGenerationRequest.OutlineRequest request) {
        
        log.info("Generating course outline for user {} course {}", userId, courseId);
        
        AiModel model = request.model() != null ? request.model() : AiModel.AMAZON_NOVA_MICRO;
        
        // Check and record quota upfront
        modelUsageService.checkAndRecordUsage(userId, model);
        
        // Create async task
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> taskRequest = Map.of(
                "courseId", courseId,
                "request", request
        );
        asyncTaskService.createTask(taskId, "OUTLINE_GENERATION", userId, taskRequest);
        
        // Submit to ai-integrations service asynchronously
        aiIntegrationsClient.generateOutlineAsync(tenantId, courseId, request.toGenerationRequest())
                .subscribe(
                        result -> asyncTaskService.completeTask(taskId, result),
                        error -> asyncTaskService.failTask(taskId, "AI_ERROR", error.getMessage())
                );
        
        return AiGenerationResponse.AsyncResponse.created(taskId);
    }
    
    /**
     * Generate content suggestions for a content item.
     * 
     * @param userId the Learn B2 user ID
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the content suggestion request
     * @return async response with task ID
     */
    public AiGenerationResponse.AsyncResponse generateContentSuggestion(
            Long userId, String tenantId, String courseId, String contentId,
            AiGenerationRequest.ContentSuggestionRequest request) {
        
        log.info("Generating content suggestion for user {} content {}", userId, contentId);
        
        AiModel model = request.model() != null ? request.model() : AiModel.AMAZON_NOVA_MICRO;
        
        // Check and record quota
        modelUsageService.checkAndRecordUsage(userId, model);
        
        // Create async task
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> taskRequest = Map.of(
                "courseId", courseId,
                "contentId", contentId,
                "request", request
        );
        asyncTaskService.createTask(taskId, "CONTENT_SUGGESTION", userId, taskRequest);
        
        // Submit to ai-integrations service
        aiIntegrationsClient.generateContentAsync(tenantId, courseId, contentId, request.toGenerationRequest())
                .subscribe(
                        result -> asyncTaskService.completeTask(taskId, result),
                        error -> asyncTaskService.failTask(taskId, "AI_ERROR", error.getMessage())
                );
        
        return AiGenerationResponse.AsyncResponse.created(taskId);
    }
    
    /**
     * Generate images for a content item.
     * 
     * @param userId the Learn B2 user ID
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the image generation request
     * @return async response with task ID
     */
    public AiGenerationResponse.AsyncResponse generateImages(
            Long userId, String tenantId, String courseId, String contentId,
            AiGenerationRequest.ImageGenerationRequest request) {
        
        log.info("Generating images for user {} content {}", userId, contentId);
        
        AiModel model = request.model() != null ? request.model() : AiModel.AMAZON_NOVA_LITE;
        
        // Check and record quota
        modelUsageService.checkAndRecordUsage(userId, model);
        
        // Create async task
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> taskRequest = Map.of(
                "courseId", courseId,
                "contentId", contentId,
                "request", request
        );
        asyncTaskService.createTask(taskId, "IMAGE_GENERATION", userId, taskRequest);
        
        // Submit to ai-integrations service
        aiIntegrationsClient.generateImagesAsync(tenantId, courseId, contentId, request.toGenerationRequest())
                .subscribe(
                        result -> asyncTaskService.completeTask(taskId, result),
                        error -> asyncTaskService.failTask(taskId, "AI_ERROR", error.getMessage())
                );
        
        return AiGenerationResponse.AsyncResponse.created(taskId);
    }
    
    /**
     * Generate flashcards for a learning activity.
     * 
     * @param userId the Learn B2 user ID
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param request the flashcard generation request
     * @return async response with task ID
     */
    public AiGenerationResponse.AsyncResponse generateFlashcards(
            Long userId, String tenantId, String courseId,
            AiGenerationRequest.FlashcardRequest request) {
        
        log.info("Generating flashcards for user {} course {}", userId, courseId);
        
        AiModel model = request.model() != null ? request.model() : AiModel.AMAZON_NOVA_MICRO;
        
        // Check and record quota
        modelUsageService.checkAndRecordUsage(userId, model);
        
        // Create async task
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> taskRequest = Map.of(
                "courseId", courseId,
                "request", request
        );
        asyncTaskService.createTask(taskId, "FLASHCARD_GENERATION", userId, taskRequest);
        
        // Submit to ai-integrations service
        aiIntegrationsClient.generateFlashcardsAsync(tenantId, courseId, request.toGenerationRequest())
                .subscribe(
                        result -> asyncTaskService.completeTask(taskId, result),
                        error -> asyncTaskService.failTask(taskId, "AI_ERROR", error.getMessage())
                );
        
        return AiGenerationResponse.AsyncResponse.created(taskId);
    }
}
