package com.blackboard.ai.client;

import com.blackboard.ai.dto.AiGenerationRequest;
import com.blackboard.ai.exception.AiServiceException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with the ai-integrations microservice.
 * 
 * <p><b>Architecture Decision:</b> Using Spring WebFlux WebClient for non-blocking
 * HTTP calls. This allows the service to handle many concurrent AI generation
 * requests without blocking threads.
 * 
 * <p><b>URL Pattern:</b>
 * {@code {baseUrl}/ai-integrations/{app}/api/v1/tenants/{tenantId}/courses/{courseId}/{endpoint}}
 * 
 * <p><b>Apps:</b>
 * <ul>
 *   <li>GENERATE - Content generation (outlines, suggestions, flashcards)</li>
 *   <li>GRADING - Grading assistance</li>
 *   <li>ATTEMPT - Student attempt feedback</li>
 * </ul>
 */
@Component
public class AiIntegrationsClient {
    
    private static final Logger log = LoggerFactory.getLogger(AiIntegrationsClient.class);
    
    private final WebClient webClient;
    private final AiIntegrationsConfig config;
    
    /**
     * Supported AI application types.
     */
    public enum AiApp {
        GENERATE, GRADING, ATTEMPT
    }
    
    public AiIntegrationsClient(AiIntegrationsConfig config) {
        this.config = config;
        this.webClient = createWebClient(config);
    }
    
    /**
     * Generate course outline asynchronously.
     * 
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param request the generation request
     * @return Mono with the response map
     */
    public Mono<Map<String, Object>> generateOutlineAsync(String tenantId, String courseId,
                                                           AiGenerationRequest.GenerationRequest request) {
        String path = buildPath(AiApp.GENERATE, tenantId, courseId, "outline");
        return postRequest(path, request);
    }
    
    /**
     * Generate content suggestions asynchronously.
     * 
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the generation request
     * @return Mono with the response map
     */
    public Mono<Map<String, Object>> generateContentAsync(String tenantId, String courseId,
                                                           String contentId,
                                                           AiGenerationRequest.GenerationRequest request) {
        String path = buildPath(AiApp.GENERATE, tenantId, courseId, "contents/" + contentId + "/suggest");
        return postRequest(path, request);
    }
    
    /**
     * Generate images asynchronously.
     * 
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param contentId the content item ID
     * @param request the generation request
     * @return Mono with the response map
     */
    public Mono<Map<String, Object>> generateImagesAsync(String tenantId, String courseId,
                                                          String contentId,
                                                          AiGenerationRequest.GenerationRequest request) {
        String path = buildPath(AiApp.GENERATE, tenantId, courseId, "contents/" + contentId + "/images");
        return postRequest(path, request);
    }
    
    /**
     * Generate flashcards asynchronously.
     * 
     * @param tenantId the tenant/institution ID
     * @param courseId the course ID
     * @param request the generation request
     * @return Mono with the response map
     */
    public Mono<Map<String, Object>> generateFlashcardsAsync(String tenantId, String courseId,
                                                              AiGenerationRequest.GenerationRequest request) {
        String path = buildPath(AiApp.GENERATE, tenantId, courseId, "learning-activities/flashcards");
        return postRequest(path, request);
    }
    
    /**
     * Health check for the ai-integrations service.
     * 
     * @return Mono with health status
     */
    public Mono<Boolean> healthCheck() {
        return webClient.get()
                .uri("/ai-integrations/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "UP".equals(response.get("status")))
                .onErrorReturn(false);
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    private String buildPath(AiApp app, String tenantId, String courseId, String endpoint) {
        return String.format("/ai-integrations/%s/api/v1/tenants/%s/courses/%s/%s",
                app.name(), tenantId, courseId, endpoint);
    }
    
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> postRequest(String path, Object body) {
        log.debug("POST request to: {}", path);
        
        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .doOnSuccess(r -> log.debug("Request to {} succeeded", path))
                .doOnError(e -> log.error("Request to {} failed: {}", path, e.getMessage()));
    }
    
    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .flatMap(body -> {
                    String errorMessage = body.containsKey("message") 
                            ? body.get("message").toString()
                            : "Unknown error from ai-integrations service";
                    
                    HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                    
                    String errorCode = switch (status) {
                        case BAD_REQUEST -> "AI_BAD_REQUEST";
                        case UNAUTHORIZED -> "AI_UNAUTHORIZED";
                        case FORBIDDEN -> "AI_FORBIDDEN";
                        case NOT_FOUND -> "AI_NOT_FOUND";
                        case TOO_MANY_REQUESTS -> "AI_RATE_LIMITED";
                        case SERVICE_UNAVAILABLE -> "AI_SERVICE_UNAVAILABLE";
                        default -> "AI_ERROR";
                    };
                    
                    return Mono.error(new AiServiceException(errorCode, errorMessage, status));
                });
    }
    
    /**
     * Create and configure the WebClient with timeouts and connection pooling.
     */
    private WebClient createWebClient(AiIntegrationsConfig config) {
        // Connection provider with pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-integrations")
                .maxConnections(config.getMaxConnections())
                .pendingAcquireMaxCount(config.getMaxPendingAcquires())
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();
        
        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(config.getReadTimeoutMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                config.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                config.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)));
        
        // Build WebClient
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        
        // Add logging filter if enabled
        if (config.isLoggingEnabled()) {
            builder.filter(logRequest())
                   .filter(logResponse());
        }
        
        return builder.build();
    }
    
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }
    
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("Response: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
