package com.example.multas.domain.port;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;

/**
 * Puerto de salida (driven port). Declara lo que el dominio necesita del
 * exterior para cobrar una multa en linea, sin saber cual proveedor concreto
 * lo resuelve ni que formato HTTP habla cada uno. Esta interfaz vive en el
 * paquete domain/ y no importa ninguna clase de Spring ni de ningun cliente
 * HTTP: es Java puro (ver Punto de decision 3 y 4 del README).
 */
public interface PasarelaPagoPort {

    ResultadoPago procesar(Multa multa);
}
