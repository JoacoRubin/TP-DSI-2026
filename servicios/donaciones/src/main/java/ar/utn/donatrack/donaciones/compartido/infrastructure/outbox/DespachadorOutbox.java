package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entrega los mensajes pendientes al servicio real. Garantía: al menos una vez (Logística es
 * idempotente por donación). Orden: por clave; si un mensaje de una donación falla, los
 * siguientes de ESA donación esperan a la próxima pasada, el resto sigue avanzando.
 * Supone una única instancia de Donaciones despachando.
 */
public class DespachadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(DespachadorOutbox.class);
    static final int LOTE = 100;

    private final BandejaSalida bandeja;
    private final ServicioLogistica logistica;
    private final ObjectMapper json;
    private final Clock reloj;

    public DespachadorOutbox(BandejaSalida bandeja, ServicioLogistica logistica, ObjectMapper json, Clock reloj) {
        this.bandeja = bandeja;
        this.logistica = logistica;
        this.json = json;
        this.reloj = reloj;
    }

    /** @return cantidad de mensajes entregados en esta pasada */
    public int despachar() {
        Set<String> bloqueadas = new HashSet<>();
        int enviados = 0;
        for (MensajeSaliente m : bandeja.pendientes(LOTE)) {
            if (bloqueadas.contains(m.clave())) {
                continue;
            }
            try {
                entregar(m);
                m.marcarEnviado(reloj.instant());
                enviados++;
            } catch (ReglaDeNegocioException e) {
                m.registrarFallo(e.getMessage(), true);
                log.error("Outbox: Logística rechazó {} #{} ({}); no se reintenta", m.tipo(), m.id(), e.getMessage());
            } catch (RuntimeException e) {
                bloqueadas.add(m.clave());
                m.registrarFallo(e.getMessage(), false);
                if (m.estado() == EstadoMensaje.FALLIDO) {
                    log.error("Outbox: {} #{} agotó {} intentos: {}", m.tipo(), m.id(), MensajeSaliente.MAX_INTENTOS, e.getMessage());
                } else {
                    log.warn("Outbox: {} #{} falló (intento {}), se reintenta: {}", m.tipo(), m.id(), m.intentos(), e.getMessage());
                }
            }
            bandeja.guardar(m);
        }
        return enviados;
    }

    private void entregar(MensajeSaliente m) {
        try {
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
