package com.blackboard.ai.entity;

import com.blackboard.ai.entity.enums.AiModel;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity tracking AI model usage per user per day.
 * 
 * <p><b>Architecture Decision:</b> Usage is tracked per user, per model, per day.
 * This allows for flexible quota management and usage analytics. The daily reset
 * is handled by the service layer checking the usage_date field.
 * 
 * <p><b>Business Rule:</b> Each model has a daily quota defined in {@link AiModel}.
 * When usage_count reaches the quota, further requests for that model are rejected.
 */
@Entity
@Table(name = "ai_playground_model_usage", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_model_date", 
        columnNames = {"users_pk1", "model_name", "usage_date"}
    ),
    indexes = {
        @Index(name = "idx_usage_user_date", columnList = "users_pk1, usage_date")
    }
)
public class ModelUsage {
    
    /**
     * Primary key - auto-generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk1")
    private Long id;
    
    /**
     * Foreign key to users table in Learn B2.
     */
    @Column(name = "users_pk1", nullable = false)
    private Long usersPk1;
    
    /**
     * The AI model being tracked.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "model_name", nullable = false, length = 50)
    private AiModel modelName;
    
    /**
     * Number of requests made for this model today.
     * Architecture Decision: Using simple counter rather than storing each request
     * to minimize storage and improve query performance.
     */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;
    
    /**
     * The date this usage record applies to.
     * Records are created per day; a new day means a fresh quota.
     */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;
    
    // ========================================================================
    // Constructors
    // ========================================================================
    
    public ModelUsage() {
        // JPA requires default constructor
    }
    
    /**
     * Create a new usage record for today with initial count of 1.
     */
    public ModelUsage(Long usersPk1, AiModel modelName) {
        this.usersPk1 = usersPk1;
        this.modelName = modelName;
        this.usageCount = 1;
        this.usageDate = LocalDate.now();
    }
    
    // ========================================================================
    // Business Methods
    // ========================================================================
    
    /**
     * Increment the usage count by 1.
     * 
     * @return the new usage count
     */
    public int incrementUsage() {
        this.usageCount++;
        return this.usageCount;
    }
    
    /**
     * Check if the quota for this model is exceeded.
     * 
     * @return true if quota is exceeded
     */
    public boolean isQuotaExceeded() {
        return modelName.isQuotaExceeded(usageCount);
    }
    
    /**
     * Get remaining quota for this model.
     * 
     * @return remaining requests available
     */
    public int getRemainingQuota() {
        return modelName.getRemainingQuota(usageCount);
    }
    
    // ========================================================================
    // Getters and Setters
    // ========================================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUsersPk1() {
        return usersPk1;
    }
    
    public void setUsersPk1(Long usersPk1) {
        this.usersPk1 = usersPk1;
    }
    
    public AiModel getModelName() {
        return modelName;
    }
    
    public void setModelName(AiModel modelName) {
        this.modelName = modelName;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    public LocalDate getUsageDate() {
        return usageDate;
    }
    
    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }
}
