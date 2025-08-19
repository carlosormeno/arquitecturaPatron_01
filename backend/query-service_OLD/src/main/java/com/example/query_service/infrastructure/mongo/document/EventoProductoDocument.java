package com.example.query_service.infrastructure.mongo.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Document(collection = "eventos_producto")
public class EventoProductoDocument {

    @Id
    private String id;
    private String productoId;
    private String evento;
    private Map<String, Object> payload;
    private Instant timestamp;
}
