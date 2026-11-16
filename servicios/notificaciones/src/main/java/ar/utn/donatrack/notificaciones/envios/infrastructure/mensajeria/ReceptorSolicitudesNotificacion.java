package ar.utn.donatrack.notificaciones.envios.infrastructure.mensajeria;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.EnviarNotificacion;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador de entrada asincrónico: consume la cola de solicitudes. */
@Component
class ReceptorSolicitudesNotificacion {

    private final EnviarNotificacion enviar;

    ReceptorSolicitudesNotificacion(EnviarNotificacion enviar) {
        this.enviar = enviar;
    }

    @JmsListener(destination = Colas.NOTIFICACIONES)
    @Transactional
    public void recibir(SolicitudNotificacion solicitud) {
        enviar.ejecutar(solicitud);
    }
}
