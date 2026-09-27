package com.example.multas.model;

/**
 * Se lanza cuando se intenta pagar (en ventanilla o en linea) una multa que
 * ya se encuentra en estado {@code PAGADA}. El {@code GlobalExceptionHandler}
 * la traduce a 409 Conflict, sin importar cual de los dos canales de pago la
 * origino.
 */
public class MultaYaPagadaException extends RuntimeException {

    public MultaYaPagadaException(String mensaje) {
        super(mensaje);
    }
}
