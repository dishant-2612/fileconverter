# Multi-stage build: Stage 1 - Build the application
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src src

# Build the application (skip tests for speed)
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -q

# Stage 2 - Runtime image with LibreOffice
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install LibreOffice and required dependencies for headless conversion
RUN apt-get update && apt-get install -y \
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    libreoffice-common \
    libsm6 \
    libxext6 \
    libxrender1 \
    libx11-dev \
    libxrandr2 \
    libfreetype6 \
    fontconfig \
    fonts-dejavu \
    fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/target/fileconverter-*.jar app.jar

# Create storage directory
RUN mkdir -p /app/storage && chmod 755 /app/storage

# Set environment variables for LibreOffice headless mode
ENV SAL_USE_VCLPLUGIN=gtk3
ENV LIBREOFFICE_PATH=/usr/bin/soffice
ENV SAL_NO_DIALOGS=1
ENV SAL_HEADLESS=1
ENV SAL_DONT_USE_FONTCONFIG=1

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/convert || exit 1

# Run the application
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
