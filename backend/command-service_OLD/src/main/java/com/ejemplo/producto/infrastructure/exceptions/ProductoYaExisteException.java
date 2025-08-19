package com.ejemplo.producto.infrastructure.exceptions;

public class ProductoYaExisteException extends ProductoDomainException{
    public ProductoYaExisteException(String message) {
        super(message);
    }
}
