package com.blackboard.ai.repository;

import com.blackboard.ai.entity.PlaygroundConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AI Playground conversations.
 * 
 * <p><b>Architecture Decision:</b> Using Spring Data JPA for standard CRUD operations
 * with custom query methods for specific business needs. The repository returns
 * entities; DTOs are created in the service layer.
 */
@Repository
public interface ConversationRepository extends JpaRepository<PlaygroundConversation, Long> {
    
    /**
     * Find a conversation by its UUID (used in API responses).
     * 
     * @param conversationUid the unique conversation identifier
     * @return the conversation if found
     */
    Optional<PlaygroundConversation> findByConversationUid(String conversationUid);
    
    /**
     * Find a conversation by UUID, ensuring it belongs to the specified user.
     * This prevents users from accessing other users' conversations.
     * 
     * @param conversationUid the unique conversation identifier
     * @param usersPk1 the user ID from Learn B2
     * @return the conversation if found and owned by the user
     */
    Optional<PlaygroundConversation> findByConversationUidAndUsersPk1(
            String conversationUid, Long usersPk1);
    
    /**
     * Find all conversations for a user with pagination.
     * Ordered by last modified date (most recent first).
     * 
     * @param usersPk1 the user ID from Learn B2
     * @param pageable pagination parameters
     * @return page of conversations
     */
    @Query("SELECT c FROM PlaygroundConversation c " +
           "WHERE c.usersPk1 = :usersPk1 " +
           "ORDER BY c.lastModifiedAt DESC")
    Page<PlaygroundConversation> findByUsersPk1(
            @Param("usersPk1") Long usersPk1, 
            Pageable pageable);
    
    /**
     * Count total conversations for a user.
     * Used for quota checks if needed.
     * 
     * @param usersPk1 the user ID from Learn B2
     * @return count of conversations
     */
    long countByUsersPk1(Long usersPk1);
    
    /**
     * Delete a conversation by its UUID and user ID.
     * Returns the number of deleted records (0 or 1).
     * 
     * @param conversationUid the unique conversation identifier
     * @param usersPk1 the user ID (ensures ownership)
     * @return number of deleted records
     */
    int deleteByConversationUidAndUsersPk1(String conversationUid, Long usersPk1);
    
    /**
     * Check if a conversation exists and belongs to a user.
     * 
     * @param conversationUid the unique conversation identifier
     * @param usersPk1 the user ID from Learn B2
     * @return true if the conversation exists and belongs to the user
     */
    boolean existsByConversationUidAndUsersPk1(String conversationUid, Long usersPk1);
}
