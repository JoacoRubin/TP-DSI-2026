package ar.utn.donatrack.donaciones.compartido.infrastructure;

import ar.utn.donatrack.donaciones.asignacion.application.EjecutarMatchmaking;
import ar.utn.donatrack.donaciones.donantes.application.NotificarDonantesInactivos;
import ar.utn.donatrack.donaciones.importacion.application.RecuperarImportacionesInterrumpidas;
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
    private final RecuperarImportacionesInterrumpidas recuperarImportaciones;

    TareasProgramadas(EjecutarMatchmaking matchmaking, NotificarDonantesInactivos inactivos,
                      RecuperarImportacionesInterrumpidas recuperarImportaciones) {
        this.matchmaking = matchmaking;
        this.inactivos = inactivos;
        this.recuperarImportaciones = recuperarImportaciones;
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

    /** Al arrancar: cerrar importaciones que un reinicio dejó EN_CURSO (nadie las va a terminar). */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void cerrarImportacionesInterrumpidas() {
        int cerradas = recuperarImportaciones.ejecutar();
        if (cerradas > 0) {
            log.warn("{} importaciones interrumpidas por un reinicio se marcaron como FALLIDAS", cerradas);
        }
    }

    @Scheduled(cron = "${donatrack.cron.inactividad:0 0 9 * * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void avisarInactivos() {
        log.info("Avisos de inactividad enviados: {}", inactivos.ejecutar());
    }
}
