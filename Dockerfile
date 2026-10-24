# Stage 1: Build with Maven and JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:21-jre-alpine

# Security: create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose the application port
EXPOSE 8080

# Healthcheck using the Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the application with JVM options for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
