package com.example.query_service.infrastructure.mongo.repository;

import com.example.query_service.infrastructure.mongo.document.ProductoHistorialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductoHistorialRepository extends MongoRepository<ProductoHistorialDocument,String> {
}
