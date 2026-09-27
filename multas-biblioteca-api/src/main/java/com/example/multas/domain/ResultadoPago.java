package com.example.multas.domain;

/**
 * Tipo de dominio devuelto por cualquier adaptador de
 * {@link com.example.multas.domain.port.PasarelaPagoPort}. Ninguna pasarela
 * concreta se expone hacia el Service: solo este contrato comun, neutral
 * respecto al formato propio de cada proveedor (ver Punto de decision 4
 * del README).
 *
 * @param proveedor          nombre de la pasarela que produjo el resultado
 *                           (por ejemplo, {@code "PAGOSUDES"} o {@code "WOMPI"}).
 * @param exitoso            {@code true} si la pasarela aprobo la transaccion.
 * @param referenciaExterna  identificador de la transaccion en la pasarela,
 *                           en el formato que cada una use internamente.
 * @param mensaje            descripcion legible del resultado, util para
 *                           propagar el motivo de un rechazo.
 */
public record ResultadoPago(
    String proveedor,
    boolean exitoso,
    String referenciaExterna,
    String mensaje
) {
}
