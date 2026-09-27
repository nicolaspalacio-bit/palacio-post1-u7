package com.example.multas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Bean de infraestructura compartido por ambos adaptadores de pago. Vive en
 * infrastructure/ y no en domain/ porque {@code RestTemplate} es, en si
 * mismo, un detalle tecnico de transporte HTTP que al dominio no le
 * corresponde conocer.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
