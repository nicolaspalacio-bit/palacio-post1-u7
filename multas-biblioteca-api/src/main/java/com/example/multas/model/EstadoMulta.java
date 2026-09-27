package com.example.multas.model;

/**
 * Estados posibles del ciclo de vida de una {@link Multa}.
 * <p>
 * Se modela como un enumerado simple y no como un patron State: ninguno de
 * los dos valores habilita comportamiento propio ni transiciones
 * condicionadas por reglas de negocio adicionales mas alla de "ya se pago o
 * no se ha pagado", por lo que introducir clases de estado aqui seria
 * complejidad sin retorno.
 */
public enum EstadoMulta {
    PENDIENTE,
    PAGADA
}
