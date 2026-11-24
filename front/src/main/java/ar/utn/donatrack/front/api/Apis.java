package ar.utn.donatrack.front.api;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

/** Punto único de acceso a los servicios de backend. */
@Component
public class Apis {

    private final ClienteApi donaciones;
    private final ClienteApi logistica;
    private final ClienteApi incentivos;
    private final ClienteApi notificaciones;
    private final ClienteApi autenticacion;

    public Apis(UrlsServicios urls, ObservationRegistry observaciones) {
        this.donaciones = new ClienteApi("donaciones", urls.donaciones(), observaciones);
        this.logistica = new ClienteApi("logística", urls.logistica(), observaciones);
        this.incentivos = new ClienteApi("incentivos", urls.incentivos(), observaciones);
        this.notificaciones = new ClienteApi("notificaciones", urls.notificaciones(), observaciones);
        this.autenticacion = new ClienteApi("autenticación", urls.autenticacion(), observaciones);
    }

    public ClienteApi donaciones() { return donaciones; }
    public ClienteApi logistica() { return logistica; }
    public ClienteApi incentivos() { return incentivos; }
    public ClienteApi notificaciones() { return notificaciones; }
    public ClienteApi autenticacion() { return autenticacion; }
}
