package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** La entidad confirma que recibió la donación y carga fotos. Queda registrado el camión. */
public class ConfirmarRecepcion {

    private final EntregaRepositorio entregas;
    private final CierreDeRuta cierre;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ConfirmarRecepcion(EntregaRepositorio entregas, CierreDeRuta cierre, PublicadorEventos eventos, Clock reloj) {
        this.entregas = entregas;
        this.cierre = cierre;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public Entrega ejecutar(UUID entregaId, List<String> fotos, Actor actor) {
        Entrega e = entregas.buscar(entregaId).orElseThrow(() -> new RecursoNoEncontradoException("Entrega", entregaId));
        actor.exigirEntidad(e.entidadId());
        Instant ahora = reloj.instant();
        e.confirmarRecepcion(fotos, ahora);
        entregas.guardar(e);
        cierre.verificar(e.rutaId(), ahora);
        eventos.publicar(new EventosLogistica.EntregaRealizada(e.id(), e.donacionId(), e.patente(), ahora));
        return e;
    }
}
