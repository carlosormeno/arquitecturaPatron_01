package com.ejemplo.producto.domain.model;

import com.ejemplo.producto.infrastructure.exceptions.ProductoDomainException;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Builder
@ToString
public class Producto {
    private final String id;
    private final String nombre;
    private final BigDecimal precio;

    // Constructor principal con validaciones
    public Producto(String id, String nombre, BigDecimal precio) {
        this.id = validarId(id);
        this.nombre = validarNombre(nombre);
        this.precio = validarPrecio(precio);
    }

    // Validaciones de dominio
    private String validarId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ProductoDomainException("El ID del producto no puede ser nulo o vacío");
        }
        if (id.length() > 36) { // UUID máximo
            throw new ProductoDomainException("El ID del producto excede la longitud máxima permitida");
        }
        return id.trim();
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ProductoDomainException("El nombre del producto no puede ser nulo o vacío");
        }
        if (nombre.trim().length() < 2) {
            throw new ProductoDomainException("El nombre del producto debe tener al menos 2 caracteres");
        }
        if (nombre.trim().length() > 100) {
            throw new ProductoDomainException("El nombre del producto no puede exceder 100 caracteres");
        }
        return nombre.trim();
    }

    private BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new ProductoDomainException("El precio del producto no puede ser nulo");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ProductoDomainException("El precio del producto no puede ser negativo");
        }
        if (precio.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ProductoDomainException("El precio del producto excede el máximo permitido");
        }
        return precio;
    }

    // Métodos de negocio
    public Producto actualizarNombre(String nuevoNombre) {
        return new Producto(this.id, nuevoNombre, this.precio);
    }

    public Producto actualizarPrecio(BigDecimal nuevoPrecio) {
        return new Producto(this.id, this.nombre, nuevoPrecio);
    }

    public boolean esPrecioMayorA(BigDecimal precio) {
        return this.precio.compareTo(precio) > 0;
    }

    public boolean esProductoCaro() {
        return this.precio.compareTo(new BigDecimal("1000")) > 0;
    }

    // Equals y hashCode basados en ID (identidad de entidad)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

