package com.ejemplo.producto.infrastructure.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponse {
    private String id;
    private String nombre;
    private BigDecimal precio;
    private String fechaCreacion;
    private String ultimaModificacion;

    // Constructor básico para compatibilidad
    public ProductoResponse(String id, String nombre, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }
}
