package com.ejemplo.producto.domain.ports.in;

import com.ejemplo.producto.domain.model.Producto;

public interface ProductoCommandUseCase {
    Producto crearProducto(Producto producto);
    Producto actualizarProducto(String id, Producto producto);
    void eliminarProducto(String id);
}
