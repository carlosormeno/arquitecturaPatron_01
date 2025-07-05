package com.ejemplo.producto.domain.ports.out;

import com.ejemplo.producto.domain.model.Producto;

public interface ActualizarProductoPort {
    Producto actualizarProducto(String id, Producto producto);
}

