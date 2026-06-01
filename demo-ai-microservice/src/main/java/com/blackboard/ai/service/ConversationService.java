package com.blackboard.ai.service;

import com.blackboard.ai.dto.ConversationDTO;
import com.blackboard.ai.entity.PlaygroundConversation;
import com.blackboard.ai.exception.AiServiceException;
import com.blackboard.ai.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing AI Playground conversations.
 * 
 * <p><b>Architecture Decision:</b> All business logic for conversations is centralized here.
 * Controllers handle HTTP concerns; this service handles domain logic.
 * 
 * <p><b>Security:</b> All operations require a user ID and enforce ownership checks.
 * Users can only access their own conversations.
 */
@Service
@Transactional
public class ConversationService {
    
    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    
    private final ConversationRepository conversationRepository;
    
    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }
    
    /**
     * Create a new conversation for a user.
     * 
     * @param userId the Learn B2 user ID
     * @param request the creation request
     * @return the created conversation
     */
    public ConversationDTO.Response createConversation(Long userId, ConversationDTO.CreateRequest request) {
        log.info("Creating new conversation for user {} with title: {}", userId, request.title());
        
        PlaygroundConversation conversation = request.toEntity(userId);
        PlaygroundConversation saved = conversationRepository.save(conversation);
        
        log.debug("Created conversation with ID: {}", saved.getConversationUid());
        return ConversationDTO.Response.fromEntity(saved);
    }
    
    /**
     * Get a conversation by ID, ensuring ownership.
     * 
     * @param userId the Learn B2 user ID
     * @param conversationId the conversation UUID
     * @return the conversation
     * @throws AiServiceException if conversation not found or not owned by user
     */
    @Transactional(readOnly = true)
    public ConversationDTO.Response getConversation(Long userId, String conversationId) {
        log.debug("Getting conversation {} for user {}", conversationId, userId);
        
        PlaygroundConversation conversation = findConversationOrThrow(userId, conversationId);
        return ConversationDTO.Response.fromEntity(conversation);
    }
    
    /**
     * List conversations for a user with pagination.
     * 
     * @param userId the Learn B2 user ID
     * @param pageable pagination parameters
     * @return paginated list of conversations
     */
    @Transactional(readOnly = true)
    public ConversationDTO.ListResponse listConversations(Long userId, Pageable pageable) {
        log.debug("Listing conversations for user {} (page: {}, size: {})", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<PlaygroundConversation> page = conversationRepository.findByUsersPk1(userId, pageable);
        
        return new ConversationDTO.ListResponse(
                page.getContent().stream()
                        .map(ConversationDTO.Response::fromEntity)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
    
    /**
     * Update a conversation (PATCH semantics - only provided fields are updated).
     * 
     * @param userId the Learn B2 user ID
     * @param conversationId the conversation UUID
     * @param request the update request
     * @return the updated conversation
     * @throws AiServiceException if conversation not found or not owned by user
     */
    public ConversationDTO.Response updateConversation(Long userId, String conversationId, 
                                                        ConversationDTO.UpdateRequest request) {
        log.info("Updating conversation {} for user {}", conversationId, userId);
        
        PlaygroundConversation conversation = findConversationOrThrow(userId, conversationId);
        
        // Apply updates (PATCH semantics)
        request.applyTo(conversation);
        
        PlaygroundConversation saved = conversationRepository.save(conversation);
        log.debug("Updated conversation {}", conversationId);
        
        return ConversationDTO.Response.fromEntity(saved);
    }
    
    /**
     * Delete a conversation.
     * 
     * @param userId the Learn B2 user ID
     * @param conversationId the conversation UUID
     * @throws AiServiceException if conversation not found or not owned by user
     */
    public void deleteConversation(Long userId, String conversationId) {
        log.info("Deleting conversation {} for user {}", conversationId, userId);
        
        // Verify existence and ownership first
        if (!conversationRepository.existsByConversationUidAndUsersPk1(conversationId, userId)) {
            throw new AiServiceException(
                    "CONVERSATION_NOT_FOUND",
                    "Conversation not found: " + conversationId,
                    HttpStatus.NOT_FOUND
            );
        }
        
        int deleted = conversationRepository.deleteByConversationUidAndUsersPk1(conversationId, userId);
        
        if (deleted == 0) {
            // Race condition - conversation was deleted between check and delete
            log.warn("Conversation {} was already deleted", conversationId);
        } else {
            log.debug("Deleted conversation {}", conversationId);
        }
    }
    
    /**
     * Get total conversation count for a user.
     * Used for usage analytics.
     * 
     * @param userId the Learn B2 user ID
     * @return count of conversations
     */
    @Transactional(readOnly = true)
    public long getConversationCount(Long userId) {
        return conversationRepository.countByUsersPk1(userId);
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    private PlaygroundConversation findConversationOrThrow(Long userId, String conversationId) {
        return conversationRepository.findByConversationUidAndUsersPk1(conversationId, userId)
                .orElseThrow(() -> new AiServiceException(
                        "CONVERSATION_NOT_FOUND",
                        "Conversation not found: " + conversationId,
                        HttpStatus.NOT_FOUND
                ));
    }
}
