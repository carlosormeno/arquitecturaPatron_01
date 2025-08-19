package com.example.query_service.infrastructure.kafka;

import com.example.query_service.domain.model.ProductoDocument;
import com.example.query_service.domain.port.out.ProductoRepository;
import com.example.query_service.infrastructure.mongo.document.EventoProductoDocument;
import com.example.query_service.infrastructure.mongo.document.ProductoHistorialDocument;
import com.example.query_service.infrastructure.mongo.repository.EventoProductoRepository;
import com.example.query_service.infrastructure.mongo.repository.ProductoHistorialRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductoKafkaConsumer {

    private final ProductoRepository productoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final ProductoHistorialRepository productoHistorialRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "dbserver1.public.producto", groupId = "query-service")
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        log.info("🔥 MENSAJE RECIBIDO EN CONSUMER: {}", record.value());
        try {
            String raw = record.value();
            if (raw == null || raw.trim().isEmpty()) {
                log.warn("⚠️ Mensaje Kafka vacío o nulo");
                return;
            }

            log.info("📥 Mensaje recibido de Kafka: {}", raw);
            JsonNode root = objectMapper.readTree(raw);

            String tipoOperacion = root.path("op").asText();
            JsonNode after = root.path("after");
            JsonNode before = root.path("before");

            // Extraer timestamp del evento Debezium
            long timestampMs = root.path("ts_ms").asLong();
            LocalDateTime eventoTimestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestampMs), ZoneOffset.UTC);

            // Validaciones específicas
            if (!isValidOperation(tipoOperacion, after, before)) {
                return;
            }

            switch (tipoOperacion) {
                case "c":
                    handleCreateOperation(after, eventoTimestamp);
                    break;
                case "u":
                    handleUpdateOperation(after, before, eventoTimestamp);
                    break;
                case "d":
                    handleDeleteOperation(before, eventoTimestamp);
                    break;
                case "r":
                    // Read operation (snapshot inicial) - tratar como create
                    handleCreateOperation(after, eventoTimestamp);
                    break;
                default:
                    log.warn("⚠️ Operación no reconocida: {}", tipoOperacion);
            }

        } catch (Exception e) {
            log.error("❌ Error procesando mensaje Kafka: {}", e.getMessage(), e);
            // En un entorno real, aquí podrías enviar a un Dead Letter Queue
            throw new RuntimeException("Error procesando evento CDC", e);
        }
    }

    private boolean isValidOperation(String tipoOperacion, JsonNode after, JsonNode before) {
        switch (tipoOperacion) {
            case "c":
            case "r":
                if (after == null || after.isNull()) {
                    log.warn("⚠️ CREATE/READ sin 'after'. Evento inválido");
                    return false;
                }
                break;
            case "u":
                if (after == null || after.isNull()) {
                    log.warn("⚠️ UPDATE sin 'after'. Evento inválido");
                    return false;
                }
                break;
            case "d":
                if (before == null || before.isNull()) {
                    log.warn("⚠️ DELETE sin 'before'. Evento inválido");
                    return false;
                }
                break;
            default:
                log.warn("⚠️ Operación desconocida: {}", tipoOperacion);
                return false;
        }
        return true;
    }

    private void handleCreateOperation(JsonNode after, LocalDateTime timestamp) {
        try {
            String id = after.path("id").asText();
            String nombre = after.path("nombre").asText();
            BigDecimal precio = extractPrecio(after);

            // Crear documento optimizado para consultas
            ProductoDocument producto = ProductoDocument.builder()
                    .id(id)
                    .nombre(nombre)
                    .precio(precio)
                    .fechaCreacion(timestamp)
                    .ultimaModificacion(timestamp)
                    .build();

            // Actualizar campos derivados para optimización de consultas
            producto.actualizarCamposDerivados();

            productoRepository.save(producto);
            guardarEvento("PRODUCTO_CREADO", objectMapper.convertValue(after, Map.class), timestamp);

            log.info("✅ Producto creado: ID={}, Nombre={}", id, nombre);

        } catch (Exception e) {
            log.error("❌ Error procesando CREATE: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleUpdateOperation(JsonNode after, JsonNode before, LocalDateTime timestamp) {
        try {
            String id = after.path("id").asText();
            String nuevoNombre = after.path("nombre").asText();
            BigDecimal nuevoPrecio = extractPrecio(after);

            // Guardar estado anterior en historial
            Optional<ProductoDocument> productoActual = productoRepository.findById(id);
            productoActual.ifPresent(p -> guardarHistorial(p, "UPDATE", timestamp));

            // Actualizar documento
            ProductoDocument producto = ProductoDocument.builder()
                    .id(id)
                    .nombre(nuevoNombre)
                    .precio(nuevoPrecio)
                    .fechaCreacion(productoActual.map(ProductoDocument::getFechaCreacion).orElse(timestamp))
                    .ultimaModificacion(timestamp)
                    .build();

            // Actualizar campos derivados
            producto.actualizarCamposDerivados();

            productoRepository.save(producto);
            guardarEvento("PRODUCTO_ACTUALIZADO", objectMapper.convertValue(after, Map.class), timestamp);

            log.info("🔄 Producto actualizado: ID={}, Nombre={}", id, nuevoNombre);

        } catch (Exception e) {
            log.error("❌ Error procesando UPDATE: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleDeleteOperation(JsonNode before, LocalDateTime timestamp) {
        try {
            String id = before.path("id").asText();

            // Guardar en historial antes de eliminar
            Optional<ProductoDocument> producto = productoRepository.findById(id);
            producto.ifPresent(p -> guardarHistorial(p, "DELETE", timestamp));

            productoRepository.deleteById(id);
            guardarEvento("PRODUCTO_ELIMINADO", objectMapper.convertValue(before, Map.class), timestamp);

            log.info("🗑️ Producto eliminado: ID={}", id);

        } catch (Exception e) {
            log.error("❌ Error procesando DELETE: {}", e.getMessage(), e);
            throw e;
        }
    }

    private BigDecimal extractPrecio(JsonNode node) {
        JsonNode precioNode = node.path("precio");
        if (precioNode.isTextual()) {
            return new BigDecimal(precioNode.asText());
        } else if (precioNode.isNumber()) {
            return precioNode.decimalValue();
        } else {
            throw new IllegalArgumentException("Formato de precio inválido: " + precioNode);
        }
    }

    private void guardarEvento(String tipoEvento, Map<String, Object> payload, LocalDateTime timestamp) {
        try {
            EventoProductoDocument evento = new EventoProductoDocument();
            evento.setProductoId((String) payload.get("id"));
            evento.setEvento(tipoEvento);
            evento.setPayload(payload);
            evento.setTimestamp(timestamp.toInstant(ZoneOffset.UTC));

            eventoProductoRepository.save(evento);
            log.debug("📝 Evento '{}' guardado para producto ID: {}", tipoEvento, payload.get("id"));

        } catch (Exception e) {
            log.error("❌ Error guardando evento: {}", e.getMessage(), e);
            // No relanzamos la excepción para que no falle el procesamiento principal
        }
    }

    private void guardarHistorial(ProductoDocument producto, String accion, LocalDateTime timestamp) {
        try {
            ProductoHistorialDocument historial = new ProductoHistorialDocument();
            historial.setProductoId(producto.getId());
            historial.setNombre(producto.getNombre());
            historial.setPrecio(producto.getPrecio());
            historial.setAccion(accion);
            historial.setTimestamp(timestamp.toInstant(ZoneOffset.UTC));

            productoHistorialRepository.save(historial);
            log.debug("📚 Historial guardado: {} para producto ID: {}", accion, producto.getId());

        } catch (Exception e) {
            log.error("❌ Error guardando historial: {}", e.getMessage(), e);
            // No relanzamos la excepción para que no falle el procesamiento principal
        }
    }
}