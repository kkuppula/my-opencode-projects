package com.blackboard.ai.entity.enums;

/**
 * Context types for AI Playground conversations.
 * 
 * <p><b>Architecture Decision:</b> Context type determines how the AI should
 * interpret and respond to user queries. This enables the AI to provide
 * more relevant responses based on the educational context.
 */
public enum ContextType {
    
    /**
     * General conversation without specific context.
     * The AI operates as a general-purpose assistant.
     */
    GENERAL("General", "Open-ended conversation"),
    
    /**
     * Course-specific context.
     * The AI has access to course materials and focuses on course content.
     */
    COURSE("Course", "Course-specific assistance"),
    
    /**
     * Assignment-focused context.
     * The AI helps with assignment-related queries while maintaining academic integrity.
     */
    ASSIGNMENT("Assignment", "Assignment and grading help"),
    
    /**
     * Content creation context.
     * The AI assists in creating educational content like quizzes, outlines, etc.
     */
    CONTENT_CREATION("Content Creation", "Creating educational materials"),
    
    /**
     * Student support context.
     * The AI focuses on helping students understand concepts and study effectively.
     */
    STUDENT_SUPPORT("Student Support", "Learning assistance and tutoring");
    
    private final String displayName;
    private final String description;
    
    ContextType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
