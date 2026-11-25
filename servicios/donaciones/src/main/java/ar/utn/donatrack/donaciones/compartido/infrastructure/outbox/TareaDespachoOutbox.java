package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Vacía la bandeja de salida periódicamente. Se apaga con donatrack.outbox.despacho-habilitado=false. */
@Component
@ConditionalOnProperty(name = "donatrack.outbox.despacho-habilitado", havingValue = "true", matchIfMissing = true)
class TareaDespachoOutbox {

    private static final Logger log = LoggerFactory.getLogger(TareaDespachoOutbox.class);
    private final DespachadorOutbox despachador;

    TareaDespachoOutbox(DespachadorOutbox despachador) {
        this.despachador = despachador;
    }

    @Scheduled(fixedDelayString = "${donatrack.outbox.intervalo-ms:3000}", initialDelayString = "${donatrack.outbox.intervalo-ms:3000}")
    void despachar() {
        try {
            int enviados = despachador.despachar();
            if (enviados > 0) {
                log.info("Outbox: {} mensajes entregados (Logística y colas)", enviados);
            }
        } catch (RuntimeException e) {
            log.error("Outbox: falló la pasada de despacho: {}", e.getMessage());
        }
    }
}
