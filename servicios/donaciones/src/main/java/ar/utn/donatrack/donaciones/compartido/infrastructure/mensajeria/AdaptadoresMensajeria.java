package ar.utn.donatrack.donaciones.compartido.infrastructure.mensajeria;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.compartido.infrastructure.outbox.PublicadorColasConOutbox;
import org.springframework.stereotype.Component;

/** Las notificaciones salen por la cola vía outbox: se confirman o se descartan junto con el caso de uso. */
@Component
class NotificadorPorCola implements Notificador {

    private final PublicadorColasConOutbox colas;

    NotificadorPorCola(PublicadorColasConOutbox colas) {
        this.colas = colas;
    }

    @Override
    public void notificar(Destinatario destinatario, String asunto, String mensaje, String tipo) {
        colas.publicar(Colas.NOTIFICACIONES, SolicitudNotificacion.de(destinatario, asunto, mensaje, tipo, "donaciones"));
    }
}

/** Eventos a Incentivos vía outbox: una donación confirmada nunca se queda sin su evento. */
@Component
class PublicadorEventosPorCola implements PublicadorEventos {

    private final PublicadorColasConOutbox colas;

    PublicadorEventosPorCola(PublicadorColasConOutbox colas) {
        this.colas = colas;
    }

    @Override
    public void donacionRegistrada(EventosDonaciones.DonacionRegistrada evento) {
        colas.publicar(Colas.INCENTIVOS_DESDE_DONACIONES, evento);
    }

    @Override
    public void donacionEntregada(EventosDonaciones.DonacionEntregada evento) {
        colas.publicar(Colas.INCENTIVOS_DESDE_DONACIONES, evento);
    }
}
