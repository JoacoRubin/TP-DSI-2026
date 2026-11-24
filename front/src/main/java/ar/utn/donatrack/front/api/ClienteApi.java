package ar.utn.donatrack.front.api;

import io.micrometer.observation.ObservationRegistry;
import ar.utn.donatrack.front.sesion.UsuarioSesion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cliente HTTP genérico hacia UN servicio. Propaga el JWT del usuario logueado (guardado en la
 * sesión del servidor: el navegador nunca lo ve) y traduce errores a {@link ErrorApi}.
 */
public class ClienteApi {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() { };
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String servicio;
    private final RestClient cliente;

    public ClienteApi(String servicio, String urlBase, ObservationRegistry observaciones) {
        this.servicio = servicio;
        // Cliente HTTP del JDK: a diferencia de HttpURLConnection, expone el cuerpo de las
        // respuestas 401 (necesario para mostrar el detalle del Problem Detail).
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build());
        f.setReadTimeout(Duration.ofSeconds(20));
        this.cliente = RestClient.builder()
                .observationRegistry(observaciones) // propaga traceparent: trazas distribuidas
                .baseUrl(urlBase)
                .requestFactory(f)
                .requestInterceptor((req, body, ex) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof UsuarioSesion u) {
                        req.getHeaders().setBearerAuth(u.token());
                    }
                    return ex.execute(req, body);
                })
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                    throw traducir(res.getStatusCode().value(), new String(res.getBody().readAllBytes()));
                })
                .build();
    }

    private <T> T invocar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponible(servicio, e);
        }
    }

    public Map<String, Object> get(String uri, Object... vars) {
        return invocar(() -> cliente.get().uri(uri, vars).retrieve().body(MAPA));
    }

    public List<Map<String, Object>> lista(String uri, Object... vars) {
        return invocar(() -> {
            List<Map<String, Object>> r = cliente.get().uri(uri, vars).retrieve().body(LISTA);
            return r == null ? List.of() : r;
        });
    }

    public String texto(String uri) {
        return invocar(() -> cliente.get().uri(uri).retrieve().body(String.class));
    }

    public Map<String, Object> post(String uri, Object cuerpo, Object... vars) {
        return invocar(() -> cliente.post().uri(uri, vars).contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo == null ? Map.of() : cuerpo).retrieve().body(MAPA));
    }

    public List<Map<String, Object>> postLista(String uri, Object cuerpo, Object... vars) {
        return invocar(() -> cliente.post().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).body(cuerpo)
                .retrieve().body(LISTA));
    }

    public Map<String, Object> multipart(String uri, MultiValueMap<String, ?> partes, Object... vars) {
        return invocar(() -> cliente.post().uri(uri, vars).contentType(MediaType.MULTIPART_FORM_DATA).body(partes)
                .retrieve().body(MAPA));
    }

    public Map<String, Object> put(String uri, Object cuerpo, Object... vars) {
        return invocar(() -> cliente.put().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).body(cuerpo)
                .retrieve().body(MAPA));
    }

    public Map<String, Object> patch(String uri, Object cuerpo, Object... vars) {
        return invocar(() -> cliente.patch().uri(uri, vars).contentType(MediaType.APPLICATION_JSON).body(cuerpo)
                .retrieve().body(MAPA));
    }

    public void delete(String uri, Object... vars) {
        invocar(() -> cliente.delete().uri(uri, vars).retrieve().toBodilessEntity());
    }

    /** Proxy de bytes (fotos): el navegador no habla directo con los servicios. */
    public byte[] bytes(String uri, Object... vars) {
        return invocar(() -> cliente.get().uri(uri, vars).retrieve().body(byte[].class));
    }

    private ErrorApi traducir(int estado, String cuerpo) {
        String mensaje = switch (estado) {
            case 401 -> "Tu sesión expiró. Volvé a iniciar sesión.";
            case 403 -> "No tenés permisos para realizar esta acción.";
            case 404 -> "No encontramos lo que buscabas.";
            default -> "Ocurrió un error inesperado (" + estado + ")";
        };
        Map<String, String> errores = new HashMap<>();
        try {
            JsonNode n = JSON.readTree(cuerpo);
            if (n.hasNonNull("detail")) {
                mensaje = n.get("detail").asText();
            }
            if (n.has("errores")) {
                n.get("errores").fields().forEachRemaining(e -> errores.put(e.getKey(), e.getValue().asText()));
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON
        }
        return new ErrorApi(estado, mensaje, errores);
    }
}
