package com.blackboard.ai.repository;

import com.blackboard.ai.entity.ModelUsage;
import com.blackboard.ai.entity.enums.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AI model usage tracking.
 * 
 * <p><b>Architecture Decision:</b> Usage is tracked per user, per model, per day.
 * Queries are optimized for the common operations:
 * <ul>
 *   <li>Check current usage before allowing a request</li>
 *   <li>Increment usage after a successful request</li>
 *   <li>Get all usage for dashboard/quota display</li>
 * </ul>
 */
@Repository
public interface ModelUsageRepository extends JpaRepository<ModelUsage, Long> {
    
    /**
     * Find usage record for a specific user, model, and date.
     * Primary method for checking quota before processing requests.
     * 
     * @param usersPk1 the user ID from Learn B2
     * @param modelName the AI model
     * @param usageDate the date to check
     * @return usage record if exists
     */
    Optional<ModelUsage> findByUsersPk1AndModelNameAndUsageDate(
            Long usersPk1, AiModel modelName, LocalDate usageDate);
    
    /**
     * Find all usage records for a user on a specific date.
     * Used to display quota dashboard across all models.
     * 
     * @param usersPk1 the user ID from Learn B2
     * @param usageDate the date to check
     * @return list of usage records (one per model used)
     */
    List<ModelUsage> findByUsersPk1AndUsageDate(Long usersPk1, LocalDate usageDate);
    
    /**
     * Get total usage count for a user across all models today.
     * Useful for overall usage analytics.
     * 
     * @param usersPk1 the user ID from Learn B2
     * @param usageDate the date to check
     * @return total usage count
     */
    @Query("SELECT COALESCE(SUM(u.usageCount), 0) FROM ModelUsage u " +
           "WHERE u.usersPk1 = :usersPk1 AND u.usageDate = :usageDate")
    int getTotalUsageForDate(
            @Param("usersPk1") Long usersPk1, 
            @Param("usageDate") LocalDate usageDate);
    
    /**
     * Get usage history for a user over a date range.
     * Used for usage analytics and reporting.
     * 
     * @param usersPk1 the user ID from Learn B2
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @return list of usage records
     */
    @Query("SELECT u FROM ModelUsage u " +
           "WHERE u.usersPk1 = :usersPk1 " +
           "AND u.usageDate BETWEEN :startDate AND :endDate " +
           "ORDER BY u.usageDate DESC, u.modelName")
    List<ModelUsage> findUsageHistory(
            @Param("usersPk1") Long usersPk1,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * Delete old usage records for data retention.
     * Called by scheduled cleanup job.
     * 
     * @param cutoffDate delete records older than this date
     * @return number of deleted records
     */
    int deleteByUsageDateBefore(LocalDate cutoffDate);
}
