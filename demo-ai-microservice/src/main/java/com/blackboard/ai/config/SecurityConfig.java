package com.blackboard.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the AI Services microservice.
 * 
 * <p><b>Architecture Decision:</b> Using OAuth2 Resource Server with JWT validation.
 * JWTs are issued by the Learn authentication service and validated against
 * the configured JWKS endpoint.
 * 
 * <p><b>Authentication Flow:</b>
 * <ol>
 *   <li>API Gateway validates Learn session</li>
 *   <li>Gateway issues/forwards JWT to this service</li>
 *   <li>This service validates JWT signature against JWKS</li>
 *   <li>User ID is extracted from JWT claims</li>
 * </ol>
 * 
 * @author Blackboard AI Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * Security filter chain for production environment.
     * Requires valid JWT for all /v1/** endpoints.
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)
                
                // Stateless session (no server-side session)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        
                        // OPTIONS requests for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // All API endpoints require authentication
                        .requestMatchers("/v1/**").authenticated()
                        
                        // Deny all other requests
                        .anyRequest().denyAll()
                )
                
                // OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        
        return http.build();
    }
    
    /**
     * Security filter chain for development environment.
     * More permissive for testing without JWT infrastructure.
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow all requests in dev mode
                        .anyRequest().permitAll()
                );
        
        return http.build();
    }
    
    /**
     * JWT decoder for production.
     * Configured via application.yml with issuer-uri.
     */
    @Bean
    @Profile("!dev")
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
    
    /**
     * JWT decoder for development.
     * Accepts any JWT for testing purposes.
     */
    @Bean
    @Profile("dev")
    public JwtDecoder devJwtDecoder() {
        // For dev, create a permissive decoder
        // In real dev, you might use a local Keycloak or mock JWT issuer
        return token -> {
            // Parse JWT manually for dev purposes
            // This is NOT secure and only for local development
            try {
                com.nimbusds.jwt.SignedJWT signedJWT = 
                        com.nimbusds.jwt.SignedJWT.parse(token);
                
                var claims = signedJWT.getJWTClaimsSet();
                
                return org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject(claims.getSubject())
                        .issuedAt(claims.getIssueTime() != null ? 
                                claims.getIssueTime().toInstant() : java.time.Instant.now())
                        .expiresAt(claims.getExpirationTime() != null ? 
                                claims.getExpirationTime().toInstant() : 
                                java.time.Instant.now().plusSeconds(3600))
                        .claim("learn_user_id", claims.getClaim("learn_user_id"))
                        .claim("tenant_id", claims.getClaim("tenant_id"))
                        .build();
            } catch (Exception e) {
                throw new org.springframework.security.oauth2.jwt.JwtException(
                        "Invalid JWT token: " + e.getMessage(), e);
            }
        };
    }
    
    /**
     * Custom JWT authentication converter.
     * Extracts authorities from JWT claims if needed.
     */
    private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter 
            jwtAuthenticationConverter() {
        var converter = new org.springframework.security.oauth2.server.resource.authentication
                .JwtAuthenticationConverter();
        
        // If you need to extract roles/authorities from JWT claims, configure here
        // For this service, we primarily use the user ID from claims, not roles
        
        return converter;
    }
}
