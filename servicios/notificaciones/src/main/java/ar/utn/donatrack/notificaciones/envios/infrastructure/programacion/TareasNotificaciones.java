package ar.utn.donatrack.notificaciones.envios.infrastructure.programacion;

import ar.utn.donatrack.notificaciones.envios.application.ReintentarFallidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TareasNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(TareasNotificaciones.class);
    private final ReintentarFallidas reintentar;

    TareasNotificaciones(ReintentarFallidas reintentar) {
        this.reintentar = reintentar;
    }

    @Scheduled(fixedDelayString = "${donatrack.notificaciones.reintento-cada:PT1M}")
    void reintentarFallidas() {
        int n = reintentar.ejecutar();
        if (n > 0) {
            log.info("Reintentadas {} notificaciones fallidas", n);
        }
    }
}
