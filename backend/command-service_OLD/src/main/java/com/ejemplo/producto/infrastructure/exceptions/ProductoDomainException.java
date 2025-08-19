package com.ejemplo.producto.infrastructure.exceptions;

public class ProductoDomainException extends RuntimeException  {
    public ProductoDomainException(String message) {
        super(message);
    }

    public ProductoDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
