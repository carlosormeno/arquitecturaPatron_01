package com.ejemplo.producto.infrastructure.persistence;

import com.ejemplo.producto.application.mapper.ProductoMapper;
import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.domain.ports.out.ActualizarProductoPort;
import com.ejemplo.producto.domain.ports.out.EliminarProductoPort;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPort;
import com.ejemplo.producto.infrastructure.exceptions.ProductoNotFoundException;
import com.ejemplo.producto.infrastructure.exceptions.ProductoPersistenceException;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductoJpaAdapter implements GuardarProductoPort, ActualizarProductoPort, EliminarProductoPort {

    private static final Logger logger = LoggerFactory.getLogger(ProductoJpaAdapter.class);
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public Producto guardarProducto(Producto producto) {
        try {
            logger.debug("Guardando producto con ID: {}", producto.getId());

            // Verificar si ya existe un producto con el mismo ID
            if (productoRepository.existsById(producto.getId())) {
                throw new ProductoPersistenceException(
                        "Ya existe un producto con el ID: " + producto.getId()
                );
            }

            ProductoJpaEntity entity = ProductoMapper.toEntity(producto);
            ProductoJpaEntity savedEntity = productoRepository.save(entity);

            logger.info("Producto guardado exitosamente: ID={}, Nombre={}",
                    savedEntity.getId(), savedEntity.getNombre());

            return ProductoMapper.toDomain(savedEntity);

        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad de datos al guardar producto: {}", e.getMessage());
            throw new ProductoPersistenceException(
                    "Error de integridad de datos al guardar el producto", e
            );
        } catch (Exception e) {
            logger.error("Error inesperado al guardar producto: {}", e.getMessage(), e);
            throw new ProductoPersistenceException(
                    "Error inesperado al guardar el producto", e
            );
        }
    }

    @Override
    @Transactional
    public Producto actualizarProducto(String id, Producto producto) {
        try {
            logger.debug("Actualizando producto con ID: {}", id);

            ProductoJpaEntity entityExistente = productoRepository.findById(id)
                    .orElseThrow(() -> new ProductoNotFoundException(
                            "No se encontró el producto con ID: " + id
                    ));

            // Actualizar campos
            entityExistente.setNombre(producto.getNombre());
            entityExistente.setPrecio(producto.getPrecio());

            ProductoJpaEntity savedEntity = productoRepository.save(entityExistente);

            logger.info("Producto actualizado exitosamente: ID={}, Nombre={}",
                    savedEntity.getId(), savedEntity.getNombre());

            return ProductoMapper.toDomain(savedEntity);

        } catch (ProductoNotFoundException e) {
            throw e; // Re-lanzar excepción de negocio
        } catch (OptimisticLockingFailureException e) {
            logger.warn("Conflicto de concurrencia al actualizar producto ID: {}", id);
            throw new ProductoPersistenceException(
                    "El producto fue modificado por otro usuario. Intente nuevamente.", e
            );
        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad de datos al actualizar producto: {}", e.getMessage());
            throw new ProductoPersistenceException(
                    "Error de integridad de datos al actualizar el producto", e
            );
        } catch (Exception e) {
            logger.error("Error inesperado al actualizar producto: {}", e.getMessage(), e);
            throw new ProductoPersistenceException(
                    "Error inesperado al actualizar el producto", e
            );
        }
    }

    @Override
    @Transactional
    public void eliminarProducto(String id) {
        try {
            logger.debug("Eliminando producto con ID: {}", id);

            if (!productoRepository.existsById(id)) {
                throw new ProductoNotFoundException(
                        "No se encontró el producto con ID: " + id
                );
            }

            productoRepository.deleteById(id);

            logger.info("Producto eliminado exitosamente: ID={}", id);

        } catch (ProductoNotFoundException e) {
            throw e; // Re-lanzar excepción de negocio
        } catch (DataIntegrityViolationException e) {
            logger.error("Error de integridad al eliminar producto: {}", e.getMessage());
            throw new ProductoPersistenceException(
                    "No se puede eliminar el producto debido a referencias existentes", e
            );
        } catch (Exception e) {
            logger.error("Error inesperado al eliminar producto: {}", e.getMessage(), e);
            throw new ProductoPersistenceException(
                    "Error inesperado al eliminar el producto", e
            );
        }
    }
}