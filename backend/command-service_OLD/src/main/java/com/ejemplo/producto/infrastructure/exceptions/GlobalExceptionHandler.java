package com.ejemplo.producto.infrastructure.exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductoNotFound(ProductoNotFoundException e) {
        logger.warn("Producto no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                        "error", "Producto no encontrado",
                        "codigo", "PRODUCTO_NO_ENCONTRADO",
                        "mensaje", e.getMessage()
                )
        );
    }

    @ExceptionHandler(ProductoDomainException.class)
    public ResponseEntity<Map<String, Object>> handleProductoDomain(ProductoDomainException e) {
        logger.warn("Error de dominio: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "error", "Error de validación de negocio",
                        "codigo", "DOMINIO_ERROR",
                        "mensaje", e.getMessage()
                )
        );
    }

    @ExceptionHandler(ProductoPersistenceException.class)
    public ResponseEntity<Map<String, Object>> handleProductoPersistence(ProductoPersistenceException e) {
        logger.error("Error de persistencia: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "error", "Error al procesar la solicitud",
                        "codigo", "PERSISTENCIA_ERROR"
                        // NO incluimos e.getMessage() porque puede contener info sensible de BD
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String nombreCampo = ((FieldError) error).getField();
            String mensajeError = error.getDefaultMessage();
            errores.put(nombreCampo, mensajeError);
        });

        logger.warn("Errores de validación: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "error", "Datos de entrada inválidos",
                        "codigo", "VALIDACION_ERROR",
                        "errores", errores
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        logger.error("Error no controlado: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "error", "Error interno del servidor",
                        "codigo", "ERROR_INTERNO"
                        // NO incluimos detalles técnicos
                )
        );
    }

}
