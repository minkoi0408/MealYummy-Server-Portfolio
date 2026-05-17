# ==============================================================================
# STAGE 1: Build the Spring Boot application using Maven & Java 21
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

# Copy Maven dependency descriptor from the meal-service module
COPY meal-service/pom.xml .

# Download dependencies offline to speed up subsequent builds (leverages Docker layer cache)
RUN mvn dependency:go-offline -B

# Copy the application source code
COPY meal-service/src ./src

# Clean and package the application into a lightweight executable jar file, skipping tests
RUN mvn clean package -DskipTests

# ==============================================================================
# STAGE 2: Lightweight runtime environment (Alpine Linux + Eclipse Temurin JRE 21)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root system user/group for running the application securely in production
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the packaged executable jar from Stage 1 (build) with proper owner permissions
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

# Expose backend port (customizable via SERVER_PORT, default 8082)
EXPOSE 8082

# Configure production-ready JVM tuning parameters
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -Xms512m -Xmx1024m"

# Execute the application safely
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
