-- =============================================================================
-- AI Services Microservice - Initial Database Schema
-- Version: V1
-- Description: Creates tables for AI Playground conversations and model usage tracking
-- =============================================================================

-- Architecture Decision: Using BIGSERIAL for primary keys to match Learn B2 conventions
-- The pk1 naming convention is maintained for compatibility with existing Learn data models

-- =============================================================================
-- Table: ai_playground_conversation
-- Stores AI Playground conversation metadata
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_playground_conversation (
    -- Primary key (matches Learn B2 naming convention)
    pk1 BIGSERIAL PRIMARY KEY,
    
    -- Foreign key to Learn users table
    -- Note: No FK constraint as this references the Learn B2 database
    users_pk1 BIGINT NOT NULL,
    
    -- UUID for external references (API responses, etc.)
    -- Using UUID string for compatibility across systems
    conversation_uid VARCHAR(36) NOT NULL UNIQUE,
    
    -- User-provided conversation title
    title VARCHAR(255) NOT NULL,
    
    -- Context type determines how AI interprets queries
    -- Stored as string for flexibility and readability
    context_type VARCHAR(50) NOT NULL,
    
    -- AI model selected for this conversation
    model_name VARCHAR(50) NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_context_type CHECK (
        context_type IN ('GENERAL', 'COURSE', 'ASSIGNMENT', 'CONTENT_CREATION', 'STUDENT_SUPPORT')
    ),
    CONSTRAINT chk_model_name CHECK (
        model_name IN ('AMAZON_NOVA_MICRO', 'AMAZON_NOVA_LITE', 'OPEN_AI_GPT_OSS_20B')
    )
);

-- Index for user lookups (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_conversation_user 
    ON ai_playground_conversation(users_pk1);

-- Index for user + date ordering (for list queries)
CREATE INDEX IF NOT EXISTS idx_conversation_user_date 
    ON ai_playground_conversation(users_pk1, last_modified_at DESC);

-- Unique index for UUID lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_conversation_uid 
    ON ai_playground_conversation(conversation_uid);

-- =============================================================================
-- Table: ai_playground_model_usage
-- Tracks AI model usage per user per day for quota enforcement
-- =============================================================================
CREATE TABLE IF NOT EXISTS ai_playground_model_usage (
    -- Primary key
    pk1 BIGSERIAL PRIMARY KEY,
    
    -- Foreign key to Learn users table
    users_pk1 BIGINT NOT NULL,
    
    -- AI model being tracked
    model_name VARCHAR(50) NOT NULL,
    
    -- Number of requests made for this model today
    usage_count INTEGER NOT NULL DEFAULT 0,
    
    -- The date this usage record applies to
    -- Using DATE type for efficient daily queries
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- Constraints
    CONSTRAINT chk_usage_model_name CHECK (
        model_name IN ('AMAZON_NOVA_MICRO', 'AMAZON_NOVA_LITE', 'OPEN_AI_GPT_OSS_20B')
    ),
    CONSTRAINT chk_usage_count_positive CHECK (usage_count >= 0),
    
    -- Unique constraint: one record per user, per model, per day
    CONSTRAINT uk_user_model_date UNIQUE (users_pk1, model_name, usage_date)
);

-- Index for quota checks (user + model + date)
CREATE INDEX IF NOT EXISTS idx_usage_user_model_date 
    ON ai_playground_model_usage(users_pk1, model_name, usage_date);

-- Index for user dashboard queries
CREATE INDEX IF NOT EXISTS idx_usage_user_date 
    ON ai_playground_model_usage(users_pk1, usage_date);

-- Index for cleanup jobs (delete old records)
CREATE INDEX IF NOT EXISTS idx_usage_date 
    ON ai_playground_model_usage(usage_date);

-- =============================================================================
-- Comments for documentation
-- =============================================================================
COMMENT ON TABLE ai_playground_conversation IS 
    'Stores AI Playground conversation sessions. Each conversation belongs to a single user.';

COMMENT ON COLUMN ai_playground_conversation.users_pk1 IS 
    'Foreign key to Learn users table (users.pk1). No FK constraint as table is in different DB.';

COMMENT ON COLUMN ai_playground_conversation.conversation_uid IS 
    'UUID exposed in API responses. Using string format for cross-system compatibility.';

COMMENT ON COLUMN ai_playground_conversation.context_type IS 
    'Determines how AI interprets user queries: GENERAL, COURSE, ASSIGNMENT, CONTENT_CREATION, STUDENT_SUPPORT';

COMMENT ON TABLE ai_playground_model_usage IS 
    'Tracks AI model usage per user per day for quota enforcement. Quotas reset daily at midnight UTC.';

COMMENT ON COLUMN ai_playground_model_usage.usage_date IS 
    'The calendar date (UTC) this usage record applies to. New records created for each day.';

-- =============================================================================
-- Trigger function for updating last_modified_at
-- =============================================================================
CREATE OR REPLACE FUNCTION update_last_modified_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for conversation table
DROP TRIGGER IF EXISTS trg_conversation_last_modified ON ai_playground_conversation;
CREATE TRIGGER trg_conversation_last_modified
    BEFORE UPDATE ON ai_playground_conversation
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_at();
