package ar.utn.donatrack.donaciones.compartido.infrastructure.integraciones;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

/**
 * Traduce errores de otros servicios a excepciones de nuestro dominio: un 4xx remoto con un
 * Problem Detail se convierte en regla de negocio con el mismo mensaje; un servicio caído en
 * un error de disponibilidad explícito.
 */
final class ErroresRemotos {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ErroresRemotos() {
    }

    static <T> T invocar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            throw new ReglaDeNegocioException(detalle(e));
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("El servicio de " + servicio + " no está disponible. Intentá nuevamente en unos minutos.", e);
        }
    }

    private static String detalle(HttpClientErrorException e) {
        try {
            JsonNode n = JSON.readTree(e.getResponseBodyAsString());
            if (n.hasNonNull("detail")) {
                return n.get("detail").asText();
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON
        }
        return e.getStatusText();
    }
}
