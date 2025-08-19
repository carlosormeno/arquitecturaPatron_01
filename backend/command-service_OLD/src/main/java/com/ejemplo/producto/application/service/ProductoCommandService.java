package com.ejemplo.producto.application.service;

import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.domain.ports.in.ProductoCommandUseCase;
import com.ejemplo.producto.domain.ports.out.ActualizarProductoPort;
import com.ejemplo.producto.domain.ports.out.EliminarProductoPort;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPort;
import com.ejemplo.producto.infrastructure.exceptions.ProductoDomainException;
import com.ejemplo.producto.infrastructure.exceptions.ProductoValidacionException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductoCommandService implements ProductoCommandUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProductoCommandService.class);

    private final GuardarProductoPort guardarProductoPort;
    private final EliminarProductoPort eliminarProductoPort;
    private final ActualizarProductoPort actualizarProductoPort;

    @Override
    @WithSpan("guardar-producto")
    public Producto crearProducto(Producto producto) {
        logger.info("Iniciando creación de producto con ID: {}", producto.getId());

        // Validaciones de negocio
        validarProducto(producto);
        validarProductoParaCreacion(producto);

        Producto productoCreado = guardarProductoPort.guardarProducto(producto);
        logger.info("Producto creado exitosamente con ID: {}", productoCreado.getId());

        return productoCreado;
    }

    @Override
    @WithSpan("actualizar-producto")
    public Producto actualizarProducto(String id, Producto producto) {
        logger.info("Iniciando actualización de producto con ID: {}", id);

        // Validaciones
        validarId(id);
        validarProducto(producto);

        // Asegurar que el ID del producto coincida con el parámetro
        if (!id.equals(producto.getId())) {
            logger.warn("ID del parámetro ({}) no coincide con ID del producto ({})", id, producto.getId());
            producto = Producto.builder()
                    .id(id)
                    .nombre(producto.getNombre())
                    .precio(producto.getPrecio())
                    .build();
        }

        Producto productoActualizado = actualizarProductoPort.actualizarProducto(id, producto);
        logger.info("Producto actualizado exitosamente con ID: {}", id);

        return productoActualizado;
    }

    @Override
    @WithSpan("eliminar-producto")
    public void eliminarProducto(String id) {
        logger.info("Iniciando eliminación de producto con ID: {}", id);

        validarId(id);

        eliminarProductoPort.eliminarProducto(id);
        logger.info("Producto eliminado exitosamente con ID: {}", id);
    }

    // Métodos privados de validación
    private void validarProducto(Producto producto) {
        if (producto == null) {
            throw new ProductoValidacionException("El producto no puede ser nulo");
        }

        validarNombre(producto.getNombre());
        validarPrecio(producto.getPrecio());
    }

    private void validarProductoParaCreacion(Producto producto) {
        if (!StringUtils.hasText(producto.getId())) {
            throw new ProductoValidacionException("El ID del producto es requerido para la creación");
        }
    }

    private void validarId(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ProductoValidacionException("El ID del producto no puede estar vacío");
        }

        // Validar formato UUID si es necesario
        if (id.length() < 3) {
            throw new ProductoValidacionException("El ID del producto debe tener al menos 3 caracteres");
        }
    }

    private void validarNombre(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            throw new ProductoValidacionException("El nombre del producto es requerido");
        }

        if (nombre.length() < 2) {
            throw new ProductoValidacionException("El nombre del producto debe tener al menos 2 caracteres");
        }

        if (nombre.length() > 100) {
            throw new ProductoValidacionException("El nombre del producto no puede exceder 100 caracteres");
        }
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new ProductoValidacionException("El precio del producto es requerido");
        }

        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductoValidacionException("El precio del producto debe ser mayor a cero");
        }

        if (precio.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new ProductoValidacionException("El precio del producto no puede exceder 999,999.99");
        }

        // Validar que tenga máximo 2 decimales
        if (precio.scale() > 2) {
            throw new ProductoValidacionException("El precio del producto no puede tener más de 2 decimales");
        }
    }
}
