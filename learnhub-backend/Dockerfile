# Multi-stage build for Spring Boot Backend
# Production-ready with health checks, security, and JVM tuning

# Stage 1: Build JAR with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy POM and source
COPY pom.xml .
COPY src ./src

# Build JAR - download dependencies and compile in one step
# Skip tests, ensure Lombok annotation processors run, disable incremental compilation
RUN mvn clean package \
  -DskipTests \
  -Dmaven.test.skip=true \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
  -q

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="LearnHub DevOps Team"
LABEL version="1.0.0"
LABEL description="LearnHub Backend - Real-time Assessment Integration"

WORKDIR /app

# Install curl for health checks and timezone data
RUN apk add --no-cache curl tzdata

# Copy built JAR from builder stage
COPY --from=builder /build/target/learnhub-backend-*.jar app.jar

# Create non-root user for security
RUN addgroup -S learnhub && adduser -S learnhub -G learnhub
USER learnhub

# Expose API port
EXPOSE 8080

# Health check endpoint (Spring Boot Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/health || exit 1

# JVM optimization for production
ENV JAVA_OPTS="\
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:InitialHeapSize=512M \
  -XX:MaxHeapSize=2G \
  -Dfile.encoding=UTF-8 \
  -Dspring.profiles.active=prod \
  -Dspring.jpa.hibernate.ddl-auto=validate"

# Application startup
ENTRYPOINT ["java", "-jar", "app.jar"]
