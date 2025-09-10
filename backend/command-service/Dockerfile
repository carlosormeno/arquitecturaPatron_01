# ==================================
# Etapa 1: construir el .jar
# ==================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better dependency caching
COPY pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build with optimizations
RUN mvn clean package -DskipTests

# ==================================
# Etapa 2: imagen de ejecución con Java + OTEL Agent
# ==================================
FROM eclipse-temurin:21-jre-alpine

# Install wget for health checks
RUN apk add --no-cache wget

# Crear usuario no root (excelente práctica de seguridad)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copiar el .jar compilado con ownership correcto
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Copiar el agente OTEL con ownership correcto
COPY --chown=spring:spring opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

# Switch to non-root user
USER spring:spring

# Variables necesarias para el agente OTEL (optimizadas)
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_SERVICE_NAME="command-service"
ENV OTEL_SERVICE_VERSION="1.0.0"
#ENV OTEL_RESOURCE_ATTRIBUTES="service.name=command-service,service.version=1.0.0,environment=docker"
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=command-service,service.version=1.0.0,environment=development"

# ✅ Configuración de exportadores
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4318"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="none"

# ✅ Configuración de sampling y instrumentación
ENV OTEL_TRACES_SAMPLER="always_on"
ENV OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED="true"
ENV OTEL_INSTRUMENTATION_HTTP_ENABLED="true"
ENV OTEL_INSTRUMENTATION_JDBC_ENABLED="true"

# JVM optimizations específicas para command service
ENV JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200 -XX:+OptimizeStringConcat"

# ✅ NUEVO: Debug JVM y Spring Boot
#ENV JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200 -XX:+OptimizeStringConcat -Dlogging.level.root=INFO -Dspring.main.banner-mode=console"

#ENV JVM_OPTS="-Xms256m -Xmx512m"

# ✅ AGREGAR estas líneas para debug:
ENV SPRING_PROFILES_ACTIVE=dev
ENV DEBUG=true
ENV LOGGING_LEVEL_ROOT=INFO
ENV SERVER_PORT=8081

# Exponer el puerto del microservicio
EXPOSE 8081

# Health check para Docker (Spring Boot Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Start with JVM optimizations
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS $JAVA_TOOL_OPTIONS -jar app.jar"]