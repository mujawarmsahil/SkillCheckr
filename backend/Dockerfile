# ==============================================================================
# Multi-stage Dockerfile for SkillCheckr Spring Boot Backend
# Java: OpenJDK 21 (Eclipse Temurin)
# Framework: Spring Boot 3.4.4
# Target Platform: Render Web Service (Container Runtime)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package the Application
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency definition and linting config first for Docker cache optimization
COPY pom.xml .
COPY checkstyle.xml .

# Download dependencies in an isolated layer
RUN mvn dependency:go-offline -B || true

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Run as non-privileged system user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy repackaged executable jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Render dynamically sets PORT at runtime; 8080 serves as default fallback
ENV PORT=8080
EXPOSE 8080

# Configure container startup with quick entropy generation
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
