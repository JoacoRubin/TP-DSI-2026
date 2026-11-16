package ar.utn.donatrack.front.api;

import org.springframework.stereotype.Component;

/** Punto único de acceso a los servicios de backend. */
@Component
public class Apis {

    private final ClienteApi donaciones;
    private final ClienteApi logistica;
    private final ClienteApi incentivos;
    private final ClienteApi notificaciones;
    private final ClienteApi autenticacion;

    public Apis(UrlsServicios urls) {
        this.donaciones = new ClienteApi("donaciones", urls.donaciones());
        this.logistica = new ClienteApi("logística", urls.logistica());
        this.incentivos = new ClienteApi("incentivos", urls.incentivos());
        this.notificaciones = new ClienteApi("notificaciones", urls.notificaciones());
        this.autenticacion = new ClienteApi("autenticación", urls.autenticacion());
    }

    public ClienteApi donaciones() { return donaciones; }
    public ClienteApi logistica() { return logistica; }
    public ClienteApi incentivos() { return incentivos; }
    public ClienteApi notificaciones() { return notificaciones; }
    public ClienteApi autenticacion() { return autenticacion; }
}
