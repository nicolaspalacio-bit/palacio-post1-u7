package com.example.multas.model;

/**
 * Se lanza cuando se solicita una multa por un identificador que no existe
 * en el repositorio. El {@code GlobalExceptionHandler} de la capa de
 * presentacion la traduce a 404 Not Found.
 */
public class MultaNotFoundException extends RuntimeException {

    public MultaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
