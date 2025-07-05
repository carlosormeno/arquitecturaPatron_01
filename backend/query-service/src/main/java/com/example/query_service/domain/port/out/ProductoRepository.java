package com.example.query_service.domain.port.out;

import com.example.query_service.domain.model.ProductoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends MongoRepository<ProductoDocument, String> {
}
