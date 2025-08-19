package com.ejemplo.producto.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
//@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:8000"})
public class LogControllerFront {

    // ✅ Logger específico para logs del frontend
    private static final Logger frontendLogger = LoggerFactory.getLogger("frontend-logs");
    private static final Logger logger = LoggerFactory.getLogger(LogControllerFront.class);

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/frontend")
    public ResponseEntity<?> receiveFrontendLog(@RequestBody Map<String, Object> logEntry) {
        frontendLogger.info("Esta es una prueba en el controller del FrontEnd - frontendLogger");
        logger.info("Esta es una prueba en el controller del FrontEnd - logger");
        try {
            // ✅ Extraer información del log
            String level = (String) logEntry.getOrDefault("level", "info");
            String message = (String) logEntry.getOrDefault("message", "");
            String traceId = (String) logEntry.get("traceId");
            String spanId = (String) logEntry.get("spanId");
            String userId = (String) logEntry.get("userId");
            String correlationId = (String) logEntry.get("correlationId");

            // ✅ Configurar MDC para correlación
            if (traceId != null) MDC.put("traceId", traceId);
            if (spanId != null) MDC.put("spanId", spanId);
            if (userId != null) MDC.put("userId", userId);
            if (correlationId != null) MDC.put("correlationId", correlationId);

            // ✅ Crear mensaje estructurado
            String logMessage = objectMapper.writeValueAsString(logEntry);

            // ✅ Log según el nivel
            switch (level.toLowerCase()) {
                case "error":
                    frontendLogger.error("Frontend: {}", logMessage);
                    break;
                case "warn":
                    frontendLogger.warn("Frontend: {}", logMessage);
                    break;
                case "debug":
                    frontendLogger.debug("Frontend: {}", logMessage);
                    break;
                default:
                    frontendLogger.info("Frontend: {}", logMessage);
            }

            logger.debug("Frontend log received and processed successfully");

            //return ResponseEntity.ok("Log received");

            // ✅ CAMBIO IMPORTANTE: Devolver JSON en lugar de String
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Log received and processed");
            response.put("timestamp", new Date().toInstant());
            response.put("correlationId", correlationId);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            logger.error("Error processing frontend log", e);
            //return ResponseEntity.status(500).body("Error processing log");

            // ✅ Error response también en JSON
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error processing log: " + e.getMessage());
            errorResponse.put("timestamp", new Date().toInstant());

            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);

        } finally {
            // ✅ Limpiar MDC
            MDC.clear();
        }
    }

    @PostMapping("/frontend/batch")
    public ResponseEntity<?> receiveFrontendLogBatch(@RequestBody Map<String, Object> request) {
        frontendLogger.info("Esta es una prueba en el controller del FrontEnd batch - frontendLogger");
        logger.info("Esta es una prueba en el controller del FrontEnd batch - logger");
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> logs = (java.util.List<Map<String, Object>>) request.get("logs");

            /*if (logs == null || logs.isEmpty()) {
                return ResponseEntity.badRequest().body("No logs provided");
            }*/

            if (logs == null || logs.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "No logs provided");
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorResponse);
            }

            int processedCount = 0;
            int errorCount = 0;

            for (Map<String, Object> logEntry : logs) {
                try {
                    // ✅ Procesar cada log individual
                    receiveFrontendLog(logEntry);
                    processedCount++;
                } catch (Exception e) {
                    logger.warn("Failed to process individual log in batch", e);
                    errorCount++;
                }
            }

            logger.info("Processed {} out of {} frontend logs in batch", processedCount, logs.size());
            //return ResponseEntity.ok(String.format("Processed %d logs", processedCount));
            // ✅ Respuesta JSON estructurada
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", String.format("Processed %d logs", processedCount));
            response.put("totalLogs", logs.size());
            response.put("processedCount", processedCount);
            response.put("errorCount", errorCount);
            response.put("timestamp", new Date().toInstant());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            logger.error("Error processing frontend log batch", e);
            //return ResponseEntity.status(500).body("Error processing log batch");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error processing log batch: " + e.getMessage());
            errorResponse.put("timestamp", new Date().toInstant());

            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    // ✅ Método helper para procesar logs individuales
    private void processIndividualLog(Map<String, Object> logEntry) throws Exception {
        String level = (String) logEntry.getOrDefault("level", "info");
        String traceId = (String) logEntry.get("traceId");
        String spanId = (String) logEntry.get("spanId");
        String userId = (String) logEntry.get("userId");
        String correlationId = (String) logEntry.get("correlationId");

        if (traceId != null) MDC.put("traceId", traceId);
        if (spanId != null) MDC.put("spanId", spanId);
        if (userId != null) MDC.put("userId", userId);
        if (correlationId != null) MDC.put("correlationId", correlationId);

        try {
            String logMessage = objectMapper.writeValueAsString(logEntry);

            switch (level.toLowerCase()) {
                case "error":
                    frontendLogger.error("Frontend: {}", logMessage);
                    break;
                case "warn":
                    frontendLogger.warn("Frontend: {}", logMessage);
                    break;
                case "debug":
                    frontendLogger.debug("Frontend: {}", logMessage);
                    break;
                default:
                    frontendLogger.info("Frontend: {}", logMessage);
            }
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        //return ResponseEntity.ok("Log endpoint is healthy");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "log-endpoint");
        response.put("timestamp", new Date().toInstant());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}
