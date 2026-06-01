package com.blackboard.ai.entity;

import com.blackboard.ai.entity.enums.AiModel;
import com.blackboard.ai.entity.enums.ContextType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an AI Playground conversation.
 * 
 * <p><b>Architecture Decision:</b> Conversations are stored with minimal metadata.
 * The actual message history is managed by the ai-integrations service to keep
 * this service lightweight and focused on orchestration.
 * 
 * <p><b>Database Mapping:</b> Maps to the {@code ai_playground_conversation} table
 * from the original Learn B2 schema.
 */
@Entity
@Table(name = "ai_playground_conversation", indexes = {
    @Index(name = "idx_conversation_user", columnList = "users_pk1"),
    @Index(name = "idx_conversation_uid", columnList = "conversation_uid", unique = true)
})
public class PlaygroundConversation {
    
    /**
     * Primary key - matches Learn B2 naming convention (pk1).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk1")
    private Long id;
    
    /**
     * Foreign key to users table in Learn B2.
     * Architecture Decision: We store the Learn user ID rather than the JWT subject
     * to maintain compatibility with existing data migration paths.
     */
    @Column(name = "users_pk1", nullable = false)
    private Long usersPk1;
    
    /**
     * Unique identifier for external references (API responses, etc.).
     * Using UUID for security (non-guessable) and distributed ID generation.
     */
    @Column(name = "conversation_uid", nullable = false, unique = true, length = 36)
    private String conversationUid;
    
    /**
     * User-provided title for the conversation.
     * Can be updated by the user via PATCH endpoint.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    /**
     * The context type determines how the AI interprets queries.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 50)
    private ContextType contextType;
    
    /**
     * The AI model selected for this conversation.
     * Can be changed per-message, but default is stored here.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "model_name", nullable = false, length = 50)
    private AiModel modelName;
    
    /**
     * Creation timestamp - set once on entity creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Last modification timestamp - updated on any change.
     */
    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;
    
    // ========================================================================
    // Lifecycle Callbacks
    // ========================================================================
    
    @PrePersist
    protected void onCreate() {
        if (conversationUid == null) {
            conversationUid = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        lastModifiedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = Instant.now();
    }
    
    // ========================================================================
    // Constructors
    // ========================================================================
    
    public PlaygroundConversation() {
        // JPA requires default constructor
    }
    
    public PlaygroundConversation(Long usersPk1, String title, ContextType contextType, AiModel modelName) {
        this.usersPk1 = usersPk1;
        this.title = title;
        this.contextType = contextType;
        this.modelName = modelName;
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
    
    public String getConversationUid() {
        return conversationUid;
    }
    
    public void setConversationUid(String conversationUid) {
        this.conversationUid = conversationUid;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public ContextType getContextType() {
        return contextType;
    }
    
    public void setContextType(ContextType contextType) {
        this.contextType = contextType;
    }
    
    public AiModel getModelName() {
        return modelName;
    }
    
    public void setModelName(AiModel modelName) {
        this.modelName = modelName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }
    
    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
