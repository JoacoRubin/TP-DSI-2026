package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Adaptador de {@link ServicioLogistica} que NO llama a Logística: registra el comando en la
 * bandeja de salida, dentro de la transacción del caso de uso. Si la transacción hace
 * rollback, el comando desaparece con ella; si confirma, {@link DespachadorOutbox} lo entrega.
 */
public class ServicioLogisticaConOutbox implements ServicioLogistica {

    static final String SOLICITAR_ENTREGA = "LOGISTICA_SOLICITAR_ENTREGA";
    static final String CANCELAR_ENTREGA = "LOGISTICA_CANCELAR_ENTREGA";

    private final BandejaSalida bandeja;
    private final ObjectMapper json;
    private final Clock reloj;

    public ServicioLogisticaConOutbox(BandejaSalida bandeja, ObjectMapper json, Clock reloj) {
        this.bandeja = bandeja;
        this.json = json;
        this.reloj = reloj;
    }

    @Override
    public void solicitarEntrega(SolicitudEntrega solicitud) {
        registrar(SOLICITAR_ENTREGA, solicitud.donacionId(), solicitud);
    }

    @Override
    public void cancelarEntrega(UUID donacionId) {
        registrar(CANCELAR_ENTREGA, donacionId, donacionId);
    }

    private void registrar(String tipo, UUID donacionId, Object contenido) {
        try {
            bandeja.guardar(MensajeSaliente.nuevo(tipo, donacionId.toString(), json.writeValueAsString(contenido), reloj.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el comando " + tipo, e);
        }
    }
}
