package com.ejemplo.producto.domain.ports.out;

import com.ejemplo.producto.domain.model.Producto;

public interface GuardarProductoPort {
    Producto guardarProducto(Producto producto);
}
