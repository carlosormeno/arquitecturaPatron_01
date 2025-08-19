package com.example.query_service.application.service;

import com.example.query_service.domain.model.ProductoDocument;
import com.example.query_service.domain.port.in.ConsultarProductoUseCase;
import com.example.query_service.domain.port.out.ProductoRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarProductoService implements ConsultarProductoUseCase {

    private final ProductoRepository productoRepository;

    @Override
    @Observed(name = "producto.obtenerTodosService", contextualName = "Obtener Todos desde Mongo")
    public List<ProductoDocument> obtenerTodos() {
        return productoRepository.findAll();
    }

    @Override
    @Observed(name = "producto.obtenerPorIdService", contextualName = "Obtener Producto por ID desde Mongo")
    public ProductoDocument obtenerPorId(String id) {
        return productoRepository.findById(id).orElse(null);
    }

}
