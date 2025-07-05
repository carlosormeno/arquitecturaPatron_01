package com.example.query_service.infrastructure.mongo.repository;

import com.example.query_service.infrastructure.mongo.document.EventoProductoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventoProductoRepository extends MongoRepository<EventoProductoDocument, String> {
}
