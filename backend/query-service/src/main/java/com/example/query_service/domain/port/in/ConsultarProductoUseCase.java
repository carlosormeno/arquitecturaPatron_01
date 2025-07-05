package com.example.query_service.domain.port.in;

import com.example.query_service.domain.model.ProductoDocument;

import java.util.List;

public interface ConsultarProductoUseCase {

    List<ProductoDocument> obtenerTodos();

    ProductoDocument obtenerPorId(String id);

}
