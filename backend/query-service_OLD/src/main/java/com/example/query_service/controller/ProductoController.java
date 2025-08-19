package com.example.query_service.controller;

import com.example.query_service.domain.model.ProductoDocument;
import com.example.query_service.domain.port.in.ConsultarProductoUseCase;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mongoProductos")
@RequiredArgsConstructor
public class ProductoController {

    private final ConsultarProductoUseCase consultarProductoUseCase;
    private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);

    @GetMapping
    @Observed(name = "mongo.productos.obtenerTodos", contextualName = "Consultar Todos los Productos")
    public ResponseEntity<List<ProductoDocument>> obtenerTodos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Usuario autenticado: {} consultando todos los productos", auth.getName());

        List<ProductoDocument> productos = consultarProductoUseCase.obtenerTodos();
        logger.info("Productos listados: {} para usuario: {}", productos.size(), auth.getName());
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    @Observed(name = "mongo.productos.obtenerPorId", contextualName = "Consultar Producto por ID")
    public ResponseEntity<?> obtenerPorId(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Usuario autenticado: {} consultando producto con ID: {}", auth.getName(), id);

        try {
            ProductoDocument producto = consultarProductoUseCase.obtenerPorId(id);
            if (producto != null) {
                logger.info("Producto encontrado: {} para usuario: {}", producto.getId(), auth.getName());
                return ResponseEntity.ok(producto);
            } else {
                logger.warn("Producto con ID {} no encontrado para usuario: {}", id, auth.getName());
                return ResponseEntity.status(404).body("Producto con ID " + id + " no encontrado");
            }
        }catch (Exception ex){
            log.error("❌ Error obteniendo producto con ID: {}", id, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> obtenerInfoServicio() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Usuario {} consultando información del servicio", auth.getName());

        try {
            return ResponseEntity.ok(java.util.Map.of(
                    "servicio", "Query Service - Productos",
                    "version", "1.0.0",
                    "descripcion", "Microservicio de consultas usando MongoDB",
                    "arquitectura", "Hexagonal + CQRS",
                    "base_datos", "MongoDB",
                    "sincronizacion", "Debezium CDC",
                    "usuario", auth.getName(),
                    "roles", auth.getAuthorities(),
                    "timestamp", java.time.Instant.now()
            ));
        }catch (Exception ex){
            log.error("❌ Error obteniendo información del servicio", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        log.debug("🏥 Health check del Query Service");
        try {
            return ResponseEntity.ok(java.util.Map.of(
                    "status", "UP",
                    "service", "query-service",
                    "timestamp", java.time.Instant.now(),
                    "database", "MongoDB",
                    "version", "1.0.0"
            ));
        } catch (Exception e) {
            log.error("❌ Health check failed", e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "service", "query-service",
                    "timestamp", java.time.Instant.now(),
                    "error", e.getMessage()
            ));
        }
    }
}
