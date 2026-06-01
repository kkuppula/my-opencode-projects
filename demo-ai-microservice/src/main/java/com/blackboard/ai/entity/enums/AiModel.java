package com.blackboard.ai.entity.enums;

/**
 * Supported AI Models with their daily usage quotas.
 * 
 * <p><b>Architecture Decision:</b> Quotas are defined at the enum level for simplicity
 * in this demo. In production, these would likely come from a configuration service
 * or be tenant-specific.
 * 
 * <p><b>Business Rule:</b> Users are limited to a certain number of requests per model
 * per day to manage costs and prevent abuse.
 */
public enum AiModel {
    
    /**
     * Amazon Nova Micro - Fast, cost-effective model for simple tasks.
     * Highest daily quota due to lower cost per request.
     */
    AMAZON_NOVA_MICRO("Amazon Nova Micro", 20, "Fast responses for simple tasks"),
    
    /**
     * Amazon Nova Lite - Balanced performance and capability.
     * Medium quota balancing cost and capability.
     */
    AMAZON_NOVA_LITE("Amazon Nova Lite", 15, "Balanced performance for most tasks"),
    
    /**
     * OpenAI GPT OSS 20B - High-capability open-source model.
     * Lower quota due to higher computational cost.
     */
    OPEN_AI_GPT_OSS_20B("OpenAI GPT OSS 20B", 10, "Advanced reasoning and generation");
    
    private final String displayName;
    private final int dailyQuota;
    private final String description;
    
    AiModel(String displayName, int dailyQuota, String description) {
        this.displayName = displayName;
        this.dailyQuota = dailyQuota;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDailyQuota() {
        return dailyQuota;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the given usage count exceeds this model's daily quota.
     * 
     * @param currentUsage the current usage count for today
     * @return true if quota is exceeded
     */
    public boolean isQuotaExceeded(int currentUsage) {
        return currentUsage >= dailyQuota;
    }
    
    /**
     * Calculate remaining quota for the day.
     * 
     * @param currentUsage the current usage count for today
     * @return remaining requests available (minimum 0)
     */
    public int getRemainingQuota(int currentUsage) {
        return Math.max(0, dailyQuota - currentUsage);
    }
}
