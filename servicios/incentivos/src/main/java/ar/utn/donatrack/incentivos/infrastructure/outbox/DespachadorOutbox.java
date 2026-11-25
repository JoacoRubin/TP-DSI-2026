package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Publica en el broker los mensajes pendientes. Garantía: al menos una vez (Notificaciones es
 * idempotente por id de solicitud). Orden: por cola; si un mensaje falla, los siguientes de ESA
 * cola esperan a la próxima pasada. Supone una única instancia de Incentivos despachando.
 */
public class DespachadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(DespachadorOutbox.class);
    static final int LOTE = 100;

    private final BandejaSalida bandeja;
    private final PublicadorMensajes broker;
    private final ObjectMapper json;
    private final Clock reloj;

    public DespachadorOutbox(BandejaSalida bandeja, PublicadorMensajes broker, ObjectMapper json, Clock reloj) {
        this.bandeja = bandeja;
        this.broker = broker;
        this.json = json;
        this.reloj = reloj;
    }

    /** @return cantidad de mensajes publicados en esta pasada */
    public int despachar() {
        Set<String> bloqueadas = new HashSet<>();
        int enviados = 0;
        Instant ahora = reloj.instant();
        for (MensajeSaliente m : bandeja.pendientes(LOTE)) {
            if (bloqueadas.contains(m.clave())) {
                continue;
            }
            // En espera de su reintento: bloquea la cola para que nadie se le adelante (por eso el
            // filtro va acá y no en la consulta, que salteándolo dejaría pasar al siguiente).
            if (m.enEspera(ahora)) {
                bloqueadas.add(m.clave());
                continue;
            }
            Class<?> clase = PublicadorColasConOutbox.claseDe(m.tipo());
            try {
                if (clase == null) {
                    m.registrarFallo("Tipo de mensaje desconocido: " + m.tipo(), true, ahora);
                    log.error("Outbox: {} #{} tiene un tipo desconocido; no se reintenta", m.tipo(), m.id());
                } else {
                    broker.publicar(m.clave(), json.readValue(m.payload(), clase));
                    m.marcarEnviado(ahora);
                    enviados++;
                }
            } catch (JsonProcessingException e) {
                m.registrarFallo("Payload ilegible: " + e.getOriginalMessage(), true, ahora);
                log.error("Outbox: {} #{} ilegible; no se reintenta", m.tipo(), m.id());
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
}
