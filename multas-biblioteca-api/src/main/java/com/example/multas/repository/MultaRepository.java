package com.example.multas.repository;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Capa de Infraestructura (Persistencia) de la Parte 1. La consulta derivada
 * {@code countByEstudianteIdAndEstado} resuelve en el motor de base de datos
 * ("¿cuantas tiene pendientes?") el dato que el Service necesita para su
 * propia decision de negocio ("¿se le permite generar una multa mas?"), en
 * lugar de traer todas las multas del estudiante a memoria y contarlas con
 * streams de Java (ver Punto de decision 2 del README).
 */
@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {

    List<Multa> findByEstudianteId(String estudianteId);

    long countByEstudianteIdAndEstado(String estudianteId, EstadoMulta estado);
}
