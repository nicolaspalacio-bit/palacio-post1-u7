package com.example.multas.service;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaNotFoundException;
import com.example.multas.repository.MultaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Capa de Aplicacion (Servicio). Orquesta el repositorio y concentra las
 * reglas de negocio que si requieren datos externos; no conoce ningun
 * detalle HTTP, y no es un simple passthrough del Repository: aplica el
 * limite de multas pendientes antes de delegar la persistencia.
 */
@Service
@Transactional
public class MultaService {

    private static final int LIMITE_MULTAS_PENDIENTES = 3;

    private final MultaRepository multaRepository;

    public MultaService(MultaRepository multaRepository) {
        this.multaRepository = multaRepository;
    }

    public List<Multa> listarTodas() {
        return multaRepository.findAll();
    }

    public List<Multa> listarPorEstudiante(String estudianteId) {
        return multaRepository.findByEstudianteId(estudianteId);
    }

    public Multa buscarPorId(Long id) {
        return multaRepository.findById(id)
            .orElseThrow(() -> new MultaNotFoundException("Multa " + id + " no encontrada"));
    }

    public Multa generar(String estudianteId, String concepto, int diasAtraso) {
        // Regla de negocio 1 - si requiere datos: no permitir que un
        // estudiante acumule mas del limite de multas pendientes
        // (ver Punto de decision 2 del README).
        long pendientes = multaRepository.countByEstudianteIdAndEstado(estudianteId, EstadoMulta.PENDIENTE);
        if (pendientes >= LIMITE_MULTAS_PENDIENTES) {
            throw new LimiteMultasPendientesException(
                "El estudiante " + estudianteId + " ya tiene " + pendientes
                    + " multas pendientes (limite: " + LIMITE_MULTAS_PENDIENTES + ")");
        }

        Multa multa = new Multa(estudianteId, concepto, diasAtraso);
        multa.setMonto(Multa.calcularMonto(diasAtraso)); // regla de negocio 2 - ver Punto de decision 1
        return multaRepository.save(multa);
    }

    public Multa pagarEnVentanilla(Long id) {
        Multa multa = buscarPorId(id);
        multa.marcarComoPagada("VENTANILLA");
        return multaRepository.save(multa);
    }
}
