package com.ejemplo.producto.application.mapper;

import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.infrastructure.dto.ActualizarProductoCommand;
import com.ejemplo.producto.infrastructure.dto.CrearProductoCommand;
import com.ejemplo.producto.infrastructure.dto.ProductoResponse;
import com.ejemplo.producto.infrastructure.persistence.ProductoJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ProductoMapper {

    private static final Logger logger = LoggerFactory.getLogger(ProductoMapper.class);

    // Evitar instanciación
    private ProductoMapper() {}

    public static ProductoResponse toResponse(Producto producto) {
        if (producto == null) {
            logger.warn("Intento de mapear un producto nulo a response");
            return null;
        }

        logger.debug("Mapeando producto {} a response", producto.getId());
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio()
        );
    }

    public static List<ProductoResponse> toResponseList(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) {
            return List.of();
        }

        return productos.stream()
                .map(ProductoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public static ProductoJpaEntity toEntity(Producto producto) {
        if (producto == null) {
            logger.warn("Intento de mapear un producto nulo a entity");
            return null;
        }

        logger.debug("Mapeando producto {} a entity", producto.getId());
        return ProductoJpaEntity.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .build();
    }

    public static Producto toDomain(ProductoJpaEntity entity) {
        if (entity == null) {
            logger.warn("Intento de mapear una entity nula a domain");
            return null;
        }

        logger.debug("Mapeando entity {} a domain", entity.getId());
        return Producto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .precio(entity.getPrecio())
                .build();
    }

    public static List<Producto> toDomainList(List<ProductoJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(ProductoMapper::toDomain)
                .collect(Collectors.toList());
    }

    public static Producto toDomain(CrearProductoCommand command, String id) {
        if (command == null) {
            logger.warn("Intento de mapear un CrearProductoCommand nulo a domain");
            return null;
        }

        logger.debug("Mapeando CrearProductoCommand a domain con ID: {}", id);
        return Producto.builder()
                .id(id)
                .nombre(command.getNombre())
                .precio(command.getPrecio())
                .build();
    }

    public static Producto toDomain(ActualizarProductoCommand command, String id) {
        if (command == null) {
            logger.warn("Intento de mapear un ActualizarProductoCommand nulo a domain");
            return null;
        }

        logger.debug("Mapeando ActualizarProductoCommand a domain con ID: {}", id);
        return Producto.builder()
                .id(id)
                .nombre(command.getNombre())
                .precio(command.getPrecio())
                .build();
    }
}