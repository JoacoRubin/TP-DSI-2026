package ar.utn.donatrack.notificaciones.envios.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;

import java.util.List;
import java.util.UUID;

/** Bandeja de notificaciones in-app de una persona usuaria. */
public class ConsultarNotificaciones {

    private final NotificacionRepositorio repositorio;

    public ConsultarNotificaciones(NotificacionRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Notificacion> de(String email) {
        return repositorio.deDestinatario(email.toLowerCase(), 100);
    }

    public void marcarLeida(UUID id, String emailSolicitante) {
        Notificacion n = repositorio.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Notificación", id));
        if (!n.destinatario().email().equalsIgnoreCase(emailSolicitante)) {
            throw new OperacionNoPermitidaException("La notificación no te pertenece");
        }
        n.marcarLeida();
        repositorio.guardar(n);
    }
}
