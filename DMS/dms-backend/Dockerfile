# ==================================
# Etapa 1: build del .jar
# ==================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
#COPY pom.xml .

# ---- Precalentar dependencias con los pom.xml ----
COPY pom.xml .
COPY dms-alfresco-client/pom.xml dms-alfresco-client/pom.xml
COPY dms-app/pom.xml dms-app/pom.xml

RUN mvn -q -B -DskipTests dependency:go-offline

#COPY src ./src
# ---- Luego recién copiar el código fuente ----
COPY dms-alfresco-client ./dms-alfresco-client
COPY dms-app ./dms-app

# Build del jar (sale en dms-app/target)
#RUN mvn -q -B clean package -DskipTests

# (en la etapa build)
RUN mvn -q -B -DskipTests -pl dms-app -am clean package
# si no te fías del binding, puedes forzar:
# RUN mvn -q -B -DskipTests -pl dms-app -am clean package spring-boot:repackage

# ==================================
# Etapa 2: runtime con Java + OTEL
# ==================================
FROM eclipse-temurin:21-jre-alpine

# Herramientas mínimas para healthcheck
RUN apk add --no-cache wget

# Usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Copiar el JAR
#COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Copiar el JAR del módulo app
#COPY --from=build --chown=spring:spring /app/dms-app/target/*.jar app.jar

# etapa runtime
COPY --from=build --chown=spring:spring /app/dms-app/target/*-exec.jar /app/app.jar


# ==== Opción A: copiar el agente si lo incluyes en el contexto ====
COPY --chown=spring:spring opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# ==== Opción B: descargar el agente por versión fija (recomendado) ====
#ARG OTEL_AGENT_VERSION=2.8.0
#RUN wget -q -O /app/opentelemetry-javaagent.jar \
#    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar \
# && chown spring:spring /app/opentelemetry-javaagent.jar

# Logs dir (si lo usas)
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs
USER spring:spring

# ----------- Config OTEL y JVM -----------
# Activa el agente por defecto; puedes desactivarlo con OTEL_JAVAAGENT_ENABLED=false
ENV OTEL_JAVAAGENT_ENABLED=true
# JVM "container-friendly"
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200 -XX:+OptimizeStringConcat -XX:MaxRAMPercentage=75"

# Inyección del agente (condicional en entrypoint)
ENV OTEL_SERVICE_NAME="dms-service"
ENV OTEL_SERVICE_VERSION="1.0.0"
ENV OTEL_RESOURCE_ATTRIBUTES="deployment.environment=development,service.instance.id=\$HOSTNAME"

# Exporters (HTTP/4318)
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4318"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
ENV OTEL_METRICS_EXPORTER="otlp"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_LOGS_EXPORTER="none"

# Sampler (ajusta en prod)
ENV OTEL_TRACES_SAMPLER="parentbased_traceidratio"
ENV OTEL_TRACES_SAMPLER_ARG="1.0"

# Instrumentaciones típicas
ENV OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED="true"
ENV OTEL_INSTRUMENTATION_HTTP_ENABLED="true"
ENV OTEL_INSTRUMENTATION_JDBC_ENABLED="true"

# ----------- Spring Boot -----------
ENV SPRING_PROFILES_ACTIVE=dev
ENV LOGGING_LEVEL_ROOT=INFO
ENV SERVER_PORT=8084
# Exponer puerto
EXPOSE 8084

# Healthcheck (Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# ----------- Arranque -----------
# Si OTEL_JAVAAGENT_ENABLED=true, inyecta el agente; si es "false", arranca sin él.
ENTRYPOINT ["sh", "-c", "exec java \
  $JAVA_TOOL_OPTIONS \
  $( [ \"$OTEL_JAVAAGENT_ENABLED\" = \"true\" ] && echo -javaagent:/app/opentelemetry-javaagent.jar ) \
  -Dotel.traces.sampler.arg=$OTEL_TRACES_SAMPLER_ARG \
  -jar app.jar"]
