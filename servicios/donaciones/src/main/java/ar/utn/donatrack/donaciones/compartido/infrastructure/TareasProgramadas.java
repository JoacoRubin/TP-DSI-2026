package ar.utn.donatrack.donaciones.compartido.infrastructure;

import ar.utn.donatrack.donaciones.asignacion.application.EjecutarMatchmaking;
import ar.utn.donatrack.donaciones.donantes.application.NotificarDonantesInactivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesos calendarizados en horario de baja carga (configurables por propiedad). Los cron
 * corren en la zona horaria de Buenos Aires.
 */
@Component
public class TareasProgramadas {

    private static final Logger log = LoggerFactory.getLogger(TareasProgramadas.class);
    private final EjecutarMatchmaking matchmaking;
    private final NotificarDonantesInactivos inactivos;

    TareasProgramadas(EjecutarMatchmaking matchmaking, NotificarDonantesInactivos inactivos) {
        this.matchmaking = matchmaking;
        this.inactivos = inactivos;
    }

    @Scheduled(cron = "${donatrack.cron.matchmaking:0 0 3 * * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void matchmakingNocturno() {
        log.info("Matchmaking nocturno: {} donaciones evaluadas", matchmaking.ejecutar());
    }

    /** Ejecución a demanda: no bloquea la request HTTP (responde 202 Accepted). */
    @Async
    @Transactional
    public void matchmakingAsincronico() {
        log.info("Matchmaking a demanda: {} donaciones evaluadas", matchmaking.ejecutar());
    }

    @Scheduled(cron = "${donatrack.cron.inactividad:0 0 9 * * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void avisarInactivos() {
        log.info("Avisos de inactividad enviados: {}", inactivos.ejecutar());
    }
}
