package com.blackboard.ai.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for AI service errors.
 * 
 * <p><b>Architecture Decision:</b> Using a single exception type with error codes
 * allows for consistent error handling while supporting different error scenarios.
 * The GlobalExceptionHandler converts these to appropriate HTTP responses.
 */
public class AiServiceException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    /**
     * Create an AI service exception.
     * 
     * @param errorCode machine-readable error code
     * @param message human-readable error message
     * @param httpStatus HTTP status to return
     */
    public AiServiceException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    /**
     * Create an AI service exception with a cause.
     * 
     * @param errorCode machine-readable error code
     * @param message human-readable error message
     * @param httpStatus HTTP status to return
     * @param cause underlying cause
     */
    public AiServiceException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    // ========================================================================
    // Factory Methods for Common Errors
    // ========================================================================
    
    /**
     * Create a "not found" exception.
     */
    public static AiServiceException notFound(String resourceType, String resourceId) {
        return new AiServiceException(
                resourceType.toUpperCase() + "_NOT_FOUND",
                resourceType + " not found: " + resourceId,
                HttpStatus.NOT_FOUND
        );
    }
    
    /**
     * Create a "quota exceeded" exception.
     */
    public static AiServiceException quotaExceeded(String modelName) {
        return new AiServiceException(
                "QUOTA_EXCEEDED",
                "Daily quota exceeded for " + modelName + ". Quota resets at midnight UTC.",
                HttpStatus.TOO_MANY_REQUESTS
        );
    }
    
    /**
     * Create a "bad request" exception.
     */
    public static AiServiceException badRequest(String message) {
        return new AiServiceException(
                "BAD_REQUEST",
                message,
                HttpStatus.BAD_REQUEST
        );
    }
    
    /**
     * Create an "unauthorized" exception.
     */
    public static AiServiceException unauthorized(String message) {
        return new AiServiceException(
                "UNAUTHORIZED",
                message,
                HttpStatus.UNAUTHORIZED
        );
    }
    
    /**
     * Create a "forbidden" exception.
     */
    public static AiServiceException forbidden(String message) {
        return new AiServiceException(
                "FORBIDDEN",
                message,
                HttpStatus.FORBIDDEN
        );
    }
    
    /**
     * Create an "internal error" exception.
     */
    public static AiServiceException internalError(String message, Throwable cause) {
        return new AiServiceException(
                "INTERNAL_ERROR",
                message,
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause
        );
    }
    
    /**
     * Create a "service unavailable" exception.
     */
    public static AiServiceException serviceUnavailable(String serviceName) {
        return new AiServiceException(
                "SERVICE_UNAVAILABLE",
                serviceName + " is temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}
