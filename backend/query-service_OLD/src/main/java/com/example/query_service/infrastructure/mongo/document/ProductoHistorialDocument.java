package com.example.query_service.infrastructure.mongo.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Document(collection = "productos_historial")
public class ProductoHistorialDocument {

    @Id
    private String id;

    private String productoId;
    private String nombre;
    private BigDecimal precio;
    private String accion; // UPDATE o DELETE
    private Instant timestamp;
}
