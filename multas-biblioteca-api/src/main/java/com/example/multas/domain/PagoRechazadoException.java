package com.example.multas.domain;

/**
 * Se lanza cuando la pasarela activa procesa el pago pero lo rechaza (fondos
 * insuficientes, transaccion denegada, pasarela no disponible, etc.). El
 * {@code GlobalExceptionHandler} de la capa de presentacion la traduce a
 * 402 Payment Required.
 */
public class PagoRechazadoException extends RuntimeException {

    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
