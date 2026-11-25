package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Arma el outbox: lo que registra y lo que despacha comparten la misma tabla. */
@Configuration
class ConfiguracionOutbox {

    @Bean
    PublicadorColasConOutbox publicadorColas(BandejaSalida bandeja, ObjectMapper json, Clock reloj) {
        return new PublicadorColasConOutbox(bandeja, json, reloj);
    }

    @Bean
    DespachadorOutbox despachadorOutbox(BandejaSalida bandeja, PublicadorMensajes broker, ObjectMapper json, Clock reloj) {
        return new DespachadorOutbox(bandeja, broker, json, reloj);
    }
}

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
                log.info("Outbox: {} mensajes publicados", enviados);
            }
        } catch (RuntimeException e) {
            log.error("Outbox: falló la pasada de despacho: {}", e.getMessage());
        }
    }
}
