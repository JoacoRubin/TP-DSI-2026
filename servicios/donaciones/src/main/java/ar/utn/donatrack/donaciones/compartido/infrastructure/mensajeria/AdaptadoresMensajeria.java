package ar.utn.donatrack.donaciones.compartido.infrastructure.mensajeria;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import org.springframework.stereotype.Component;

@Component
class NotificadorPorCola implements Notificador {

    private final PublicadorPostCommit publicador;

    NotificadorPorCola(PublicadorPostCommit publicador) {
        this.publicador = publicador;
    }

    @Override
    public void notificar(Destinatario destinatario, String asunto, String mensaje, String tipo) {
        publicador.publicar(Colas.NOTIFICACIONES, SolicitudNotificacion.de(destinatario, asunto, mensaje, tipo, "donaciones"));
    }
}

@Component
class PublicadorEventosPorCola implements PublicadorEventos {

    private final PublicadorPostCommit publicador;

    PublicadorEventosPorCola(PublicadorPostCommit publicador) {
        this.publicador = publicador;
    }

    @Override
    public void donacionRegistrada(EventosDonaciones.DonacionRegistrada evento) {
        publicador.publicar(Colas.INCENTIVOS_DESDE_DONACIONES, evento);
    }

    @Override
    public void donacionEntregada(EventosDonaciones.DonacionEntregada evento) {
        publicador.publicar(Colas.INCENTIVOS_DESDE_DONACIONES, evento);
    }
}
