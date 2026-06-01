package com.blackboard.ai.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the AI Services API.
 * 
 * <p><b>Architecture Decision:</b> Centralizing exception handling ensures consistent
 * error responses across all endpoints. The response format follows a standard
 * structure that clients can rely on.
 * 
 * <p><b>Error Response Format:</b>
 * <pre>
 * {
 *   "error": {
 *     "code": "ERROR_CODE",
 *     "message": "Human readable message",
 *     "timestamp": "2024-01-15T10:30:00Z",
 *     "path": "/v1/ai/conversations",
 *     "details": [...] // Optional, for validation errors
 *   }
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Standard error response format.
     */
    @Schema(description = "API error response")
    public record ErrorResponse(
            @Schema(description = "Error details")
            ErrorDetails error
    ) {
        @Schema(description = "Error detail object")
        public record ErrorDetails(
                @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR")
                String code,
                
                @Schema(description = "Human-readable error message")
                String message,
                
                @Schema(description = "When the error occurred")
                Instant timestamp,
                
                @Schema(description = "Request path that caused the error")
                String path,
                
                @Schema(description = "Additional error details (e.g., validation errors)")
                Object details
        ) {}
        
        public static ErrorResponse of(String code, String message, String path) {
            return new ErrorResponse(new ErrorDetails(code, message, Instant.now(), path, null));
        }
        
        public static ErrorResponse of(String code, String message, String path, Object details) {
            return new ErrorResponse(new ErrorDetails(code, message, Instant.now(), path, details));
        }
    }
    
    /**
     * Handle custom AI service exceptions.
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceException(
            AiServiceException ex, WebRequest request) {
        
        log.warn("AI Service Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
    
    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        
        log.warn("Validation failed: {} field errors", fieldErrors.size());
        
        ErrorResponse response = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed",
                getPath(request),
                fieldErrors
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        
        log.warn("Missing parameter: {}", ex.getParameterName());
        
        ErrorResponse response = ErrorResponse.of(
                "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing",
                getPath(request)
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle type mismatch errors (e.g., invalid enum values).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        log.warn("Type mismatch: {}", message);
        
        ErrorResponse response = ErrorResponse.of(
                "TYPE_MISMATCH",
                message,
                getPath(request)
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle malformed JSON in request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        log.warn("Malformed JSON: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                "MALFORMED_JSON",
                "Request body is not valid JSON or has invalid structure",
                getPath(request)
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle 404 errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException ex, WebRequest request) {
        
        ErrorResponse response = ErrorResponse.of(
                "ENDPOINT_NOT_FOUND",
                "The requested endpoint does not exist: " + ex.getRequestURL(),
                getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle WebClient connection errors (ai-integrations service unavailable).
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> handleWebClientRequestException(
            WebClientRequestException ex, WebRequest request) {
        
        log.error("AI Integrations service connection error: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                "AI_SERVICE_UNAVAILABLE",
                "AI generation service is temporarily unavailable. Please try again later.",
                getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * Handle WebClient response errors.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(
            WebClientResponseException ex, WebRequest request) {
        
        log.error("AI Integrations service error: {} - {}", 
                ex.getStatusCode(), ex.getResponseBodyAsString());
        
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = "AI_SERVICE_ERROR";
        String message = "AI service returned an error";
        
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            code = "AI_RATE_LIMITED";
            message = "AI service rate limit exceeded. Please try again later.";
        }
        
        ErrorResponse response = ErrorResponse.of(code, message, getPath(request));
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Catch-all handler for unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error: ", ex);
        
        ErrorResponse response = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                getPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    private String getPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
    
    private Map<String, String> formatFieldError(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                "rejectedValue", String.valueOf(error.getRejectedValue())
        );
    }
}
