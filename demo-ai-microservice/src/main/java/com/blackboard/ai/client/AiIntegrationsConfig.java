package com.blackboard.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the AI Integrations client.
 * 
 * <p><b>Architecture Decision:</b> Externalized configuration for service URLs
 * and timeouts allows different values per environment without code changes.
 */
@Configuration
@ConfigurationProperties(prefix = "ai.integrations")
public class AiIntegrationsConfig {
    
    /**
     * Base URL of the ai-integrations service.
     * Example: https://ai-integrations.blackboard.com
     */
    private String baseUrl = "http://localhost:8081";
    
    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeoutMs = 5000;
    
    /**
     * Read timeout in milliseconds.
     */
    private int readTimeoutMs = 60000;
    
    /**
     * Write timeout in milliseconds.
     */
    private int writeTimeoutMs = 30000;
    
    /**
     * Maximum number of connections in the pool.
     */
    private int maxConnections = 100;
    
    /**
     * Maximum pending acquires from the connection pool.
     */
    private int maxPendingAcquires = 1000;
    
    /**
     * Whether to enable request/response logging (for debugging).
     */
    private boolean loggingEnabled = false;
    
    // Getters and Setters
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }
    
    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }
    
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }
    
    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
    
    public int getWriteTimeoutMs() {
        return writeTimeoutMs;
    }
    
    public void setWriteTimeoutMs(int writeTimeoutMs) {
        this.writeTimeoutMs = writeTimeoutMs;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public int getMaxPendingAcquires() {
        return maxPendingAcquires;
    }
    
    public void setMaxPendingAcquires(int maxPendingAcquires) {
        this.maxPendingAcquires = maxPendingAcquires;
    }
    
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }
}
