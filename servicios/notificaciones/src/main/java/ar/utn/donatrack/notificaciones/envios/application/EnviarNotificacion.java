package ar.utn.donatrack.notificaciones.envios.application;

import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;
import ar.utn.donatrack.notificaciones.envios.domain.SelectorCanal;

import java.time.Clock;
import java.util.UUID;

/** Persiste primero y envía después: si el proveedor falla, la notificación no se pierde. */
public class EnviarNotificacion {

    private final NotificacionRepositorio repositorio;
    private final SelectorCanal selector;
    private final Clock reloj;

    public EnviarNotificacion(NotificacionRepositorio repositorio, SelectorCanal selector, Clock reloj) {
        this.repositorio = repositorio;
        this.selector = selector;
        this.reloj = reloj;
    }

    public Notificacion ejecutar(SolicitudNotificacion s) {
        UUID id = s.id() != null ? s.id() : UUID.randomUUID();
        Notificacion notificacion = repositorio.buscar(id).orElseGet(() -> Notificacion.nueva(id, s.destinatario(),
                s.asunto(), s.mensaje(), s.tipo(), s.origen(), s.medio(), reloj.instant()));
        notificacion.intentarEnvio(selector, reloj.instant());
        repositorio.guardar(notificacion);
        return notificacion;
    }
}
