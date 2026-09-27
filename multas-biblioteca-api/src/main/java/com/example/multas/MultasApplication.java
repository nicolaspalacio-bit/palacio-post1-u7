package com.example.multas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API de gestion de multas de biblioteca.
 * <p>
 * El arranque no contiene logica propia: SpringApplication.run se limita a
 * levantar el contexto y dejar que cada capa (controller, service, model,
 * repository, domain, infrastructure) se auto-configure segun sus propias
 * anotaciones. Ninguna capa depende de esta clase.
 */
@SpringBootApplication
public class MultasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultasApplication.class, args);
    }
}
