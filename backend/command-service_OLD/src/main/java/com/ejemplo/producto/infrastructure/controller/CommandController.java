package com.ejemplo.producto.infrastructure.controller;


import com.ejemplo.producto.application.mapper.ProductoMapper;
import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.domain.ports.in.ProductoCommandUseCase;
import com.ejemplo.producto.infrastructure.dto.CrearProductoCommand;
import com.ejemplo.producto.infrastructure.dto.ProductoResponse;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class CommandController {

    private final ProductoCommandUseCase productoCommandUseCase;
    private static final Logger logger = LoggerFactory.getLogger(CommandController.class);


    @PostMapping
    @Observed(name = "producto.crear", contextualName = "Crear Producto")
    public ResponseEntity<Map<String, Object>> crearProducto(@Valid @RequestBody CrearProductoCommand command) {
        String idGenerado = UUID.randomUUID().toString();
        Producto creado = productoCommandUseCase.crearProducto(ProductoMapper.toDomain(command, idGenerado));
        ProductoResponse response = ProductoMapper.toResponse(creado);

        logger.info("Producto creado exitosamente con ID: {}", idGenerado);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("mensaje", "Producto creado exitosamente", "producto", response)
        );
    }

    @PutMapping("/{id}")
    @Observed(name = "producto.actualizar", contextualName = "Actualizar Producto")
    public ResponseEntity<Map<String, Object>> actualizarProducto(@PathVariable String id,
                                                                  @Valid @RequestBody CrearProductoCommand command) {
        Producto actualizado = productoCommandUseCase.actualizarProducto(id, ProductoMapper.toDomain(command, id));
        ProductoResponse response = ProductoMapper.toResponse(actualizado);
        logger.info("Producto actualizado exitosamente con ID: {}", id);
        return ResponseEntity.ok(
                Map.of("mensaje", "Producto actualizado exitosamente", "producto", response)
        );
    }

    @DeleteMapping("/{id}")
    @Observed(name = "producto.eliminar", contextualName = "Eliminar Producto")
    public ResponseEntity<Map<String, Object>> eliminarProducto(@PathVariable String id) {
        productoCommandUseCase.eliminarProducto(id);

        logger.info("Producto eliminado exitosamente con ID: {}", id);
        return ResponseEntity.ok(
                Map.of("mensaje", "Producto eliminado exitosamente", "id", id)
        );
    }
}
