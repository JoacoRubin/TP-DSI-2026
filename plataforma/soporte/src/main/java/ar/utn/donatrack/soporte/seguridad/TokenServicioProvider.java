package ar.utn.donatrack.soporte.seguridad;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Obtiene (y cachea hasta su vencimiento) un token de servicio para las llamadas
 * servicio→servicio. Se expone como interceptor para RestClient.
 */
public class TokenServicioProvider {

    private final RestClient restClient;
    private final PropiedadesClienteServicio propiedades;
    private volatile String token;
    private volatile Instant expira = Instant.EPOCH;

    public TokenServicioProvider(RestClient restClient, PropiedadesClienteServicio propiedades) {
        this.restClient = restClient;
        this.propiedades = propiedades;
    }

    public synchronized String token() {
        if (token == null || Instant.now().isAfter(expira.minusSeconds(30))) {
            TokenRespuesta r = restClient.post()
                    .uri("/api/auth/token-servicio")
                    .body(Map.of("clientId", propiedades.id(), "clientSecret", propiedades.secreto()))
                    .retrieve()
                    .body(TokenRespuesta.class);
            if (r == null) {
                throw new IllegalStateException("Autenticación no devolvió token de servicio");
            }
            token = r.accessToken();
            expira = Instant.now().plusSeconds(r.expiraEnSegundos());
        }
        return token;
    }

    public ClientHttpRequestInterceptor interceptor() {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(token());
            return execution.execute(request, body);
        };
    }

    record TokenRespuesta(String accessToken, long expiraEnSegundos) {
    }
}
