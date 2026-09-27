package com.example.multas.model;

/**
 * Se lanza cuando un estudiante ya alcanzo el numero maximo de multas en
 * estado {@code PENDIENTE} permitido y se intenta generar una adicional.
 * El {@code GlobalExceptionHandler} la traduce a 409 Conflict.
 */
public class LimiteMultasPendientesException extends RuntimeException {

    public LimiteMultasPendientesException(String mensaje) {
        super(mensaje);
    }
}
