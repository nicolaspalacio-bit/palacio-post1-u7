package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.Multa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Adaptador de salida (driven adapter) para la pasarela piloto Wompi.
 * Traduce su contrato propio ({@code reference} / {@code status}, montos en
 * centavos) al mismo tipo de dominio {@link ResultadoPago} que produce
 * {@link PagosUdesAdapter}, aunque el formato subyacente sea completamente
 * distinto. Solo existe en el contexto de Spring cuando
 * {@code app.pagos.proveedor=wompi}.
 */
@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "wompi")
public class WompiAdapter implements PasarelaPagoPort {

    private final RestTemplate restTemplate;

    @Value("${app.pagos.wompi.url:http://localhost:9002/wompi/transactions}")
    private String urlPasarela;

    public WompiAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ResultadoPago procesar(Multa multa) {
        try {
            long montoEnCentavos = multa.getMonto().multiply(BigDecimal.valueOf(100)).longValue();
            WompiRequest request = new WompiRequest("multa-" + multa.getId(), montoEnCentavos);
            WompiResponse response = restTemplate.postForObject(urlPasarela, request, WompiResponse.class);
            boolean exitoso = response != null && "APPROVED".equalsIgnoreCase(response.status());
            return new ResultadoPago(
                "WOMPI",
                exitoso,
                response != null ? response.reference() : null,
                exitoso ? "Pago aprobado por Wompi" : "Pago rechazado por Wompi");
        } catch (RestClientException ex) {
            return new ResultadoPago("WOMPI", false, null, "Wompi no disponible: " + ex.getMessage());
        }
    }

    private record WompiRequest(String reference, long amountInCents) {
    }

    private record WompiResponse(String reference, String status) {
    }
}
