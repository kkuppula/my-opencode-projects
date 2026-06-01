# AI Services Microservice - Multi-stage Docker Build
#
# Architecture Decision: Multi-stage build for smaller production images
# Stage 1: Build with full JDK
# Stage 2: Run with minimal JRE (Eclipse Temurin for stability)

# =============================================================================
# Stage 1: Build
# =============================================================================
FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

# Copy build files first (layer caching optimization)
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Download dependencies (cached layer unless build files change)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN gradle bootJar --no-daemon -x test

# Extract layers for optimized Docker layering
# Architecture Decision: Using Spring Boot's layered JAR for better caching
RUN java -Djarmode=layertools -jar build/libs/ai-services-*.jar extract --destination extracted

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# Security: Run as non-root user
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/false appuser

WORKDIR /app

# Copy extracted layers in order of change frequency (least to most)
# This maximizes Docker layer cache efficiency
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose application port
EXPOSE 8080

# JVM tuning for containers
# Architecture Decision: Using container-aware memory settings
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Use Spring Boot's launcher for layered JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# Labels for container registry
LABEL maintainer="Blackboard AI Team" \
      version="1.0.0" \
      description="AI Services Microservice - Extracted from Learn B2" \
      org.opencontainers.image.source="https://github.com/blackboard/ai-services"
