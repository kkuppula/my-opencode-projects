package com.blackboard.ai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * <p><b>Architecture Decision:</b> Using springdoc-openapi for automatic API
 * documentation generation. This provides:
 * <ul>
 *   <li>Interactive Swagger UI at /swagger-ui.html</li>
 *   <li>OpenAPI 3.0 JSON spec at /v3/api-docs</li>
 *   <li>Automatic schema generation from DTOs</li>
 * </ul>
 * 
 * <p><b>Demo Note:</b> The Swagger UI is accessible for demonstration purposes.
 * In production, consider restricting access or disabling in certain environments.
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI Services API",
                version = "1.0.0",
                description = """
                        AI Services Microservice API - Extracted from Learn B2
                        
                        This service provides AI-powered capabilities for course content generation,
                        AI Playground conversations, and model usage tracking.
                        
                        ## Authentication
                        
                        All endpoints (except health checks and this documentation) require a valid JWT token.
                        Include the token in the Authorization header:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## Rate Limiting
                        
                        AI model usage is limited by daily quotas per user:
                        - Amazon Nova Micro: 20 requests/day
                        - Amazon Nova Lite: 15 requests/day
                        - OpenAI GPT OSS 20B: 10 requests/day
                        
                        Quotas reset at midnight UTC.
                        
                        ## Async Operations
                        
                        AI generation operations are asynchronous. They return a task ID immediately,
                        and you poll the task status endpoint to check for completion.
                        """,
                contact = @Contact(
                        name = "AI Services Team",
                        email = "ai-support@blackboard.com"
                ),
                license = @License(
                        name = "Proprietary",
                        url = "https://www.blackboard.com"
                )
        ),
        servers = {
                @Server(url = "/", description = "Current server"),
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "https://api.blackboard.com", description = "Production")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token from Learn authentication"
)
public class OpenApiConfig {
    
    /**
     * Customize OpenAPI specification.
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            // Sort schemas alphabetically for easier navigation
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Map<String, Schema> sortedSchemas = new TreeMap<>(openApi.getComponents().getSchemas());
                openApi.getComponents().setSchemas(sortedSchemas);
            }
            
            // Add additional info
            openApi.getInfo().addExtension("x-logo", Map.of(
                    "url", "https://www.blackboard.com/themes/flavor_flavor_flavor/logo.svg",
                    "altText", "Blackboard Logo"
            ));
        };
    }
    
    /**
     * Additional OpenAPI customization.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("AI Services API")
                        .version("1.0.0"))
                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                        .description("AI Services Architecture Documentation")
                        .url("https://confluence.blackboard.com/display/AI/Services"));
    }
}
