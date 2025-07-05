package com.ejemplo.producto.infrastructure.exceptions;

import org.springframework.dao.DataIntegrityViolationException;

public class ProductoPersistenceException extends RuntimeException  {
    public ProductoPersistenceException(String message) {
        super(message);
    }

    public ProductoPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
