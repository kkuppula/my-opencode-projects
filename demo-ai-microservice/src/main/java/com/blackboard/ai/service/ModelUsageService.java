package com.blackboard.ai.service;

import com.blackboard.ai.dto.ModelUsageDTO;
import com.blackboard.ai.entity.ModelUsage;
import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.exception.AiServiceException;
import com.blackboard.ai.repository.ModelUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for tracking and enforcing AI model usage quotas.
 * 
 * <p><b>Architecture Decision:</b> Quotas are enforced per user, per model, per day.
 * This provides granular control and prevents any single user from monopolizing
 * AI resources while allowing flexibility in model choice.
 * 
 * <p><b>Daily Reset:</b> Quotas reset at midnight UTC. The system creates new
 * usage records for each day rather than resetting existing records, which
 * provides natural usage history for analytics.
 */
@Service
@Transactional
public class ModelUsageService {
    
    private static final Logger log = LoggerFactory.getLogger(ModelUsageService.class);
    
    /** Number of days to retain usage history */
    private static final int USAGE_RETENTION_DAYS = 90;
    
    private final ModelUsageRepository modelUsageRepository;
    
    public ModelUsageService(ModelUsageRepository modelUsageRepository) {
        this.modelUsageRepository = modelUsageRepository;
    }
    
    /**
     * Check if a user can make a request with the specified model.
     * Does not increment usage - call {@link #recordUsage} for that.
     * 
     * @param userId the Learn B2 user ID
     * @param model the AI model to check
     * @return quota check result
     */
    @Transactional(readOnly = true)
    public ModelUsageDTO.QuotaCheckResponse checkQuota(Long userId, AiModel model) {
        LocalDate today = LocalDate.now();
        
        Optional<ModelUsage> usageOpt = modelUsageRepository
                .findByUsersPk1AndModelNameAndUsageDate(userId, model, today);
        
        int currentUsage = usageOpt.map(ModelUsage::getUsageCount).orElse(0);
        int remaining = model.getRemainingQuota(currentUsage);
        
        if (remaining > 0) {
            return ModelUsageDTO.QuotaCheckResponse.allowed(model, remaining);
        } else {
            return ModelUsageDTO.QuotaCheckResponse.denied(model);
        }
    }
    
    /**
     * Record a usage of the specified model for a user.
     * Creates a new record if none exists for today, or increments the existing count.
     * 
     * @param userId the Learn B2 user ID
     * @param model the AI model used
     * @throws AiServiceException if quota is exceeded
     */
    public void recordUsage(Long userId, AiModel model) {
        LocalDate today = LocalDate.now();
        
        Optional<ModelUsage> usageOpt = modelUsageRepository
                .findByUsersPk1AndModelNameAndUsageDate(userId, model, today);
        
        ModelUsage usage;
        if (usageOpt.isPresent()) {
            usage = usageOpt.get();
            
            // Check quota before incrementing
            if (usage.isQuotaExceeded()) {
                throw new AiServiceException(
                        "QUOTA_EXCEEDED",
                        String.format("Daily quota exceeded for %s. Quota resets at midnight UTC.", 
                                model.getDisplayName()),
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            
            usage.incrementUsage();
            log.debug("Incremented usage for user {} model {} to {}", 
                    userId, model, usage.getUsageCount());
        } else {
            // First usage of this model today
            usage = new ModelUsage(userId, model);
            log.debug("Created new usage record for user {} model {}", userId, model);
        }
        
        modelUsageRepository.save(usage);
    }
    
    /**
     * Check quota and record usage atomically.
     * Use this method when you want to ensure quota is available before processing.
     * 
     * @param userId the Learn B2 user ID
     * @param model the AI model to use
     * @throws AiServiceException if quota is exceeded
     */
    public void checkAndRecordUsage(Long userId, AiModel model) {
        ModelUsageDTO.QuotaCheckResponse check = checkQuota(userId, model);
        if (!check.allowed()) {
            throw new AiServiceException(
                    "QUOTA_EXCEEDED",
                    check.message(),
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
        recordUsage(userId, model);
    }
    
    /**
     * Get complete usage information for a user including all models.
     * 
     * @param userId the Learn B2 user ID
     * @return usage information for all models
     */
    @Transactional(readOnly = true)
    public ModelUsageDTO.UsageResponse getUsage(Long userId) {
        LocalDate today = LocalDate.now();
        List<ModelUsage> records = modelUsageRepository.findByUsersPk1AndUsageDate(userId, today);
        return ModelUsageDTO.UsageResponse.fromUsageRecords(records, today);
    }
    
    /**
     * Get usage history for a user over a date range.
     * 
     * @param userId the Learn B2 user ID
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @return list of usage records
     */
    @Transactional(readOnly = true)
    public List<ModelUsage> getUsageHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        return modelUsageRepository.findUsageHistory(userId, startDate, endDate);
    }
    
    /**
     * Scheduled job to clean up old usage records.
     * Runs daily at 2 AM UTC.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldUsageRecords() {
        LocalDate cutoffDate = LocalDate.now().minusDays(USAGE_RETENTION_DAYS);
        int deleted = modelUsageRepository.deleteByUsageDateBefore(cutoffDate);
        
        if (deleted > 0) {
            log.info("Cleaned up {} usage records older than {}", deleted, cutoffDate);
        }
    }
}
