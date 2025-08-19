package com.ejemplo.producto.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoRepository extends JpaRepository<ProductoJpaEntity, String> {

    @Query("SELECT p FROM ProductoJpaEntity p WHERE p.nombre LIKE %:nombre%")
    List<ProductoJpaEntity> findByNombreContaining(@Param("nombre") String nombre);

    @Query("SELECT p FROM ProductoJpaEntity p WHERE p.precio BETWEEN :min AND :max")
    List<ProductoJpaEntity> findByPrecioBetween(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    boolean existsByNombreIgnoreCase(String nombre);

}
