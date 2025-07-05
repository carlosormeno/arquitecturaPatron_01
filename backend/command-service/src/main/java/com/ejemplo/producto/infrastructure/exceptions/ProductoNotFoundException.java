package com.ejemplo.producto.infrastructure.exceptions;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(String message) {
        super(message);
    }
}
