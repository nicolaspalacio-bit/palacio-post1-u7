package com.example.multas.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada de la capa de Presentacion. Existe para que el Controller
 * valide la forma de la solicitud HTTP antes de llegar al Service, sin que
 * el Service (ni mucho menos el dominio) dependan de anotaciones de
 * Bean Validation atadas al transporte HTTP.
 */
public record GenerarMultaRequest(
    @NotBlank(message = "El código de estudiante es obligatorio") String estudianteId,
    @NotBlank(message = "El concepto es obligatorio") String concepto,
    @Min(value = 1, message = "Los días de atraso deben ser al menos 1") int diasAtraso
) {
}
