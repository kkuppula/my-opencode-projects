package com.blackboard.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Services Microservice Application
 * 
 * <p>This microservice extracts AI functionality from the Learn B2 monolith,
 * providing standalone capabilities for:
 * <ul>
 *   <li>AI-powered course content generation (outlines, suggestions, images, flashcards)</li>
 *   <li>AI Playground conversations with model selection</li>
 *   <li>Model usage tracking with daily quotas</li>
 *   <li>Async task status polling</li>
 * </ul>
 * 
 * <p><b>Architecture Decision:</b> This service acts as a facade/orchestrator that:
 * <ol>
 *   <li>Validates JWT tokens from the API gateway</li>
 *   <li>Manages conversation and usage state in PostgreSQL</li>
 *   <li>Delegates actual AI generation to the ai-integrations microservice</li>
 *   <li>Tracks async task status for long-running operations</li>
 * </ol>
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServicesApplication.class, args);
    }
}
