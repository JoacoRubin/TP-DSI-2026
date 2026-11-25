package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entrega los mensajes pendientes a su destino real: comandos REST a Logística y mensajes JMS
 * (notificaciones, eventos a Incentivos). Garantía: al menos una vez (Logística es idempotente
 * por donación, Notificaciones por id de solicitud e Incentivos por id de donación).
 * Orden: por clave (donación o cola); si un mensaje falla, los siguientes de ESA clave esperan a
 * la próxima pasada, el resto sigue avanzando. Supone una única instancia de Donaciones despachando.
 */
public class DespachadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(DespachadorOutbox.class);
    static final int LOTE = 100;

    private final BandejaSalida bandeja;
    private final ServicioLogistica logistica;
    private final PublicadorMensajes broker;
    private final ObjectMapper json;
    private final Clock reloj;

    public DespachadorOutbox(BandejaSalida bandeja, ServicioLogistica logistica, PublicadorMensajes broker,
                             ObjectMapper json, Clock reloj) {
        this.bandeja = bandeja;
        this.logistica = logistica;
        this.broker = broker;
        this.json = json;
        this.reloj = reloj;
    }

    /** @return cantidad de mensajes entregados en esta pasada */
    public int despachar() {
        Set<String> bloqueadas = new HashSet<>();
        int enviados = 0;
        Instant ahora = reloj.instant();
        for (MensajeSaliente m : bandeja.pendientes(LOTE)) {
            if (bloqueadas.contains(m.clave())) {
                continue;
            }
            // En espera de su reintento: bloquea su clave para que nadie de la misma clave se le adelante.
            // Por eso el filtro va acá y no en la consulta (que salteándolo dejaría pasar al siguiente).
            if (m.enEspera(ahora)) {
                bloqueadas.add(m.clave());
                continue;
            }
            try {
                entregar(m);
                m.marcarEnviado(ahora);
                enviados++;
            } catch (ReglaDeNegocioException e) {
                m.registrarFallo(e.getMessage(), true, ahora);
                log.error("Outbox: {} #{} rechazado ({}); no se reintenta", m.tipo(), m.id(), e.getMessage());
            } catch (RuntimeException e) {
                bloqueadas.add(m.clave());
                m.registrarFallo(e.getMessage(), false, ahora);
                if (m.estado() == EstadoMensaje.FALLIDO) {
                    log.error("Outbox: {} #{} agotó {} intentos: {}", m.tipo(), m.id(), MensajeSaliente.MAX_INTENTOS, e.getMessage());
                } else {
                    log.warn("Outbox: {} #{} falló (intento {}), se reintenta a las {}: {}", m.tipo(), m.id(), m.intentos(),
                            m.proximoIntento(), e.getMessage());
                }
            }
            bandeja.guardar(m);
        }
        return enviados;
    }

    private void entregar(MensajeSaliente m) {
        try {
            if (m.tipo().startsWith(PublicadorColasConOutbox.PREFIJO)) {
                Class<?> clase = PublicadorColasConOutbox.claseDe(m.tipo());
                if (clase == null) {
                    throw new ReglaDeNegocioException("Tipo de mensaje JMS desconocido: " + m.tipo());
                }
                broker.publicar(m.clave(), json.readValue(m.payload(), clase));
                return;
            }
            switch (m.tipo()) {
                case ServicioLogisticaConOutbox.SOLICITAR_ENTREGA ->
                        logistica.solicitarEntrega(json.readValue(m.payload(), ServicioLogistica.SolicitudEntrega.class));
                case ServicioLogisticaConOutbox.CANCELAR_ENTREGA ->
                        logistica.cancelarEntrega(json.readValue(m.payload(), UUID.class));
                default -> throw new ReglaDeNegocioException("Tipo de mensaje desconocido: " + m.tipo());
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ReglaDeNegocioException("Payload ilegible: " + e.getOriginalMessage());
        }
    }
}
