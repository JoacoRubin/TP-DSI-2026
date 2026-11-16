package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.Actor;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** La entidad (o el chofer) informa que la entrega no pudo concretarse. Queda para revisión. */
public class InformarNoRecepcion {

    private final EntregaRepositorio entregas;
    private final CierreDeRuta cierre;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public InformarNoRecepcion(EntregaRepositorio entregas, CierreDeRuta cierre, PublicadorEventos eventos, Clock reloj) {
        this.entregas = entregas;
        this.cierre = cierre;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public Entrega ejecutar(UUID entregaId, String motivo, Actor actor) {
        Entrega e = entregas.buscar(entregaId).orElseThrow(() -> new RecursoNoEncontradoException("Entrega", entregaId));
        if ("CHOFER".equals(actor.rol())) {
            actor.exigirChoferDe(e.patente());
        } else {
            actor.exigirEntidad(e.entidadId());
        }
        Instant ahora = reloj.instant();
        e.informarNoRecibida(motivo, ahora);
        entregas.guardar(e);
        cierre.verificar(e.rutaId(), ahora);
        eventos.publicar(new EventosLogistica.EntregaNoSatisfactoria(e.id(), e.donacionId(), e.motivoNoRecepcion(), ahora));
        return e;
    }
}
