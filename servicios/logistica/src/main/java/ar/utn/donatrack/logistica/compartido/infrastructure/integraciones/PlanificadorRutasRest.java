package ar.utn.donatrack.logistica.compartido.infrastructure.integraciones;

import io.micrometer.observation.ObservationRegistry;
import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.rutas.application.PlanificadorRutas;
import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
class PublicadorEventosPorCola implements PublicadorEventos {

    private final PublicadorMensajes publicador;

    PublicadorEventosPorCola(PublicadorMensajes publicador) {
        this.publicador = publicador;
    }

    public void publicar(EventosLogistica.EntregaPlanificada e) { publicador.publicar(Colas.DONACIONES_DESDE_LOGISTICA, e); }
    public void publicar(EventosLogistica.RutaIniciada e) { publicador.publicar(Colas.DONACIONES_DESDE_LOGISTICA, e); }
    public void publicar(EventosLogistica.EntregaRealizada e) { publicador.publicar(Colas.DONACIONES_DESDE_LOGISTICA, e); }
    public void publicar(EventosLogistica.EntregaNoSatisfactoria e) { publicador.publicar(Colas.DONACIONES_DESDE_LOGISTICA, e); }
    public void publicar(EventosLogistica.EntregaReprogramada e) { publicador.publicar(Colas.DONACIONES_DESDE_LOGISTICA, e); }
}

/**
 * Contrato con el proveedor externo: POST del lote + URL de callback y un token secreto que el
 * proveedor debe devolver en el header {@code X-Callback-Token} (autentica la respuesta).
 */
public class PlanificadorRutasRest implements PlanificadorRutas {

    private final RestClient cliente;
    private final String urlCallback;
    private final String tokenCallback;

    public PlanificadorRutasRest(String urlPlanificador, String urlCallback, String tokenCallback,
                                 ObservationRegistry observaciones) {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build());
        f.setReadTimeout(Duration.ofSeconds(10));
        this.cliente = RestClient.builder().observationRegistry(observaciones).baseUrl(urlPlanificador).requestFactory(f).build();
        this.urlCallback = urlCallback;
        this.tokenCallback = tokenCallback;
    }

    record Solicitud(UUID solicitudId, Ubicacion deposito, List<CamionDisponible> camiones,
                     List<EntregaAPlanificar> entregas, String callbackUrl, String callbackToken) {
    }

    @Override
    public void solicitar(Pedido p) {
        cliente.post().uri("/api/planificaciones")
                .body(new Solicitud(p.solicitudId(), p.deposito(), p.camiones(), p.entregas(), urlCallback, tokenCallback))
                .retrieve()
                .toBodilessEntity();
    }
}
