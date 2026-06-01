/**
 * AI Services Microservice - Build Configuration
 * 
 * This microservice extracts AI functionality from the Learn B2 monolith,
 * providing a standalone service for AI-powered course content generation,
 * playground conversations, and model usage tracking.
 * 
 * Architecture Decision: Using Kotlin DSL for type-safe, IDE-friendly build scripts.
 */

plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.blackboard"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

// Centralized version management for consistency
extra["springCloudVersion"] = "2023.0.1"

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient for non-blocking HTTP
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Security - JWT validation via OAuth2 Resource Server
    // Architecture Decision: Using standard OAuth2 resource server rather than custom JWT parsing
    // This provides automatic token validation against the configured JWKS endpoint
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    
    // AWS Integration - Secrets Manager for secure credential storage
    implementation(platform("software.amazon.awssdk:bom:2.25.27"))
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:sts") // For IAM role assumption
    
    // OpenAPI / Swagger - API documentation for demo
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    
    // Utilities
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.h2database:h2") // In-memory DB for tests
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters") // Enable parameter names for Spring
}

// Custom task to display build info during demo
tasks.register("buildInfo") {
    group = "help"
    description = "Display build configuration info"
    doLast {
        println("""
            |===========================================
            | AI Services Microservice
            |===========================================
            | Version: ${project.version}
            | Java: ${java.sourceCompatibility}
            | Spring Boot: 3.2.5
            | Spring Cloud: ${property("springCloudVersion")}
            |===========================================
        """.trimMargin())
    }
}
