package com.example.query_service.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "producto")
@CompoundIndex(def = "{'rangoPrecio': 1, 'nombreBusqueda': 1}") // Índice compuesto para filtros complejos
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDocument {

    @Id
    private String id;

    @Indexed // Para búsquedas eficientes por nombre
    private String nombre;

    @Indexed // Para filtros y ordenamiento por precio
    private BigDecimal precio;

    // Campos de auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaModificacion;

    // Campos derivados/desnormalizados para optimizar consultas
    @Indexed
    private String nombreBusqueda; // Nombre en minúsculas para búsquedas case-insensitive

    private Boolean esProductoCaro; // Pre-calculado para consultas rápidas

    @Indexed
    private String rangoPrecio; // "BAJO", "MEDIO", "ALTO" para filtrado rápido

    // Campos adicionales útiles para el query model
    private String nombreCompleto; // Nombre + ID para búsquedas únicas
    private Integer nivelPrecio; // 1-5 para rankings y comparaciones numéricas

    /**
     * Método para actualizar todos los campos derivados
     * Se llama automáticamente al establecer nombre o precio
     */
    public void actualizarCamposDerivados() {
        if (this.nombre != null) {
            this.nombreBusqueda = this.nombre.toLowerCase().trim();
            this.nombreCompleto = this.nombre + " (" + this.id + ")";
        }

        if (this.precio != null) {
            this.esProductoCaro = this.precio.compareTo(new BigDecimal("1000")) > 0;
            this.rangoPrecio = calcularRangoPrecio();
            this.nivelPrecio = calcularNivelPrecio();
        }
    }

    private String calcularRangoPrecio() {
        if (precio == null) return "INDEFINIDO";

        if (precio.compareTo(new BigDecimal("50")) < 0) {
            return "MUY_BAJO";
        } else if (precio.compareTo(new BigDecimal("200")) < 0) {
            return "BAJO";
        } else if (precio.compareTo(new BigDecimal("1000")) < 0) {
            return "MEDIO";
        } else if (precio.compareTo(new BigDecimal("5000")) < 0) {
            return "ALTO";
        } else {
            return "MUY_ALTO";
        }
    }

    private Integer calcularNivelPrecio() {
        if (precio == null) return 0;

        if (precio.compareTo(new BigDecimal("50")) < 0) {
            return 1;
        } else if (precio.compareTo(new BigDecimal("200")) < 0) {
            return 2;
        } else if (precio.compareTo(new BigDecimal("1000")) < 0) {
            return 3;
        } else if (precio.compareTo(new BigDecimal("5000")) < 0) {
            return 4;
        } else {
            return 5;
        }
    }

    // Setters personalizados para mantener consistencia de campos derivados
    public void setNombre(String nombre) {
        this.nombre = nombre;
        actualizarCamposDerivados();
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
        actualizarCamposDerivados();
    }

    // Métodos de utilidad para el query model
    public boolean coincideConBusqueda(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return true;
        }
        String terminoBusqueda = termino.toLowerCase().trim();
        return this.nombreBusqueda != null && this.nombreBusqueda.contains(terminoBusqueda);
    }

    public boolean estaEnRangoPrecio(BigDecimal min, BigDecimal max) {
        if (this.precio == null) return false;

        boolean mayorMin = min == null || this.precio.compareTo(min) >= 0;
        boolean menorMax = max == null || this.precio.compareTo(max) <= 0;

        return mayorMin && menorMax;
    }
}