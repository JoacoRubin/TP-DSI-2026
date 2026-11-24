package ar.utn.donatrack.incentivos.infrastructure.integraciones;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.incentivos.misiones.application.ProcesarDonacionEntregada;
import ar.utn.donatrack.incentivos.misiones.application.ProcesarDonacionRegistrada;
import ar.utn.donatrack.incentivos.misiones.application.Puertos;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.ranking.application.Rankings;
import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Map;

@Component
class NotificadorPorCola implements Puertos.Notificador {

    private final PublicadorMensajes publicador;

    NotificadorPorCola(PublicadorMensajes publicador) {
        this.publicador = publicador;
    }

    @Override
    public void notificar(Destinatario d, String asunto, String mensaje, String tipo) {
        SolicitudNotificacion s = SolicitudNotificacion.de(d, asunto, mensaje, tipo, "incentivos");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicador.publicar(Colas.NOTIFICACIONES, s);
                }
            });
        } else {
            publicador.publicar(Colas.NOTIFICACIONES, s);
        }
    }
}

/**
 * Integración con n8n (automatización low-code): un webhook recibe el logro, el flujo genera
 * la imagen de la insignia y publica en la red social. Si no hay URL configurada, sólo registra.
 */
@Component
class DifusorN8n implements Puertos.DifusorLogros {

    private static final Logger log = LoggerFactory.getLogger(DifusorN8n.class);
    static final Duration CONEXION = Duration.ofSeconds(2);
    static final Duration LECTURA = Duration.ofSeconds(5);
    private final String webhook;
    private final RestClient cliente;

    /**
     * HTTP/1.1 explícito: el HttpClient del JDK por defecto pide "Upgrade: h2c" y n8n se queda con esa
     * conexión sin responder. Y timeouts: esto corre en el consumidor de la cola; sin ellos, un n8n lento
     * o caído traba TODAS las misiones de todos los donantes.
     */
    DifusorN8n(@Value("${donatrack.incentivos.webhook-n8n:}") String webhook, RestClient.Builder clientes) {
        this.webhook = webhook;
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(CONEXION).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(LECTURA);
        this.cliente = clientes.requestFactory(fabrica).build(); // builder de Spring: ya instrumentado para trazas
    }

    @Override
    public void difundir(PerfilDonante perfil, InsigniaObtenida insignia, String texto) {
        Map<String, Object> cuerpo = Map.of(
                "donanteId", perfil.donanteId().toString(),
                "nombre", perfil.nombre(),
                "categoria", perfil.categoria().etiqueta(),
                "insignia", insignia.nombre(),
                "icono", insignia.icono(),
                "texto", texto,
                "obtenidaEn", insignia.obtenidaEn().toString());
        if (webhook == null || webhook.isBlank()) {
            log.info("[n8n deshabilitado] Difusión de insignia: {}", texto);
            return;
        }
        try {
            cliente.post().uri(webhook).body(cuerpo).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            // La difusión es "best effort": nunca debe impedir que el donante reciba su insignia.
            log.warn("No se pudo invocar el flujo de n8n: {}", e.getMessage());
        }
    }
}

@Component
class ReceptorEventosDonaciones {

    private final ProcesarDonacionRegistrada registrada;
    private final ProcesarDonacionEntregada entregada;
    private final org.springframework.jms.support.converter.MessageConverter conversor;

    ReceptorEventosDonaciones(ProcesarDonacionRegistrada registrada, ProcesarDonacionEntregada entregada,
                              org.springframework.jms.support.converter.MessageConverter conversor) {
        this.registrada = registrada;
        this.entregada = entregada;
        this.conversor = conversor;
    }

    /** Varios tipos de evento por cola: conversión explícita del mensaje crudo. */
    @JmsListener(destination = Colas.INCENTIVOS_DESDE_DONACIONES)
    @Transactional
    public void recibir(jakarta.jms.Message mensaje) throws jakarta.jms.JMSException {
        Object evento = conversor.fromMessage(mensaje);
        switch (evento) {
            case EventosDonaciones.DonacionRegistrada e -> registrada.ejecutar(e);
            case EventosDonaciones.DonacionEntregada e -> entregada.ejecutar(e);
            default -> LoggerFactory.getLogger(getClass()).warn("Evento ignorado: {}", evento);
        }
    }
}

/** Flujo programado de cierre de mes: persiste el ranking del mes que terminó. */
@Component
class CierreMensual {

    private static final Logger log = LoggerFactory.getLogger(CierreMensual.class);
    private final Rankings rankings;
    private final Clock reloj;

    CierreMensual(Rankings rankings, Clock reloj) {
        this.rankings = rankings;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${donatrack.cron.ranking:0 10 0 1 * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void generarRankingDelMesAnterior() {
        YearMonth anterior = YearMonth.now(reloj).minusMonths(1);
        log.info("Ranking {} generado con {} donantes", anterior, rankings.generar(anterior).entradas().size());
    }
}
