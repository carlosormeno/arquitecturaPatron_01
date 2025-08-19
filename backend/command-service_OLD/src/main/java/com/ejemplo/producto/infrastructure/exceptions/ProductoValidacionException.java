package com.ejemplo.producto.infrastructure.exceptions;

public class ProductoValidacionException extends ProductoDomainException{
    public ProductoValidacionException(String message) {
        super(message);
    }
}
