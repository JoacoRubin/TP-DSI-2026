package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.logistica.compartido.application.PublicadorEventos;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;

import java.time.Clock;
import java.util.UUID;

/** Revisado el caso, la donación volvió al depósito: la entrega vuelve a "Pendiente" para replanificarse. */
public class ReprogramarEntrega {

    private final EntregaRepositorio entregas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ReprogramarEntrega(EntregaRepositorio entregas, PublicadorEventos eventos, Clock reloj) {
        this.entregas = entregas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public Entrega ejecutar(UUID entregaId) {
        Entrega e = entregas.buscar(entregaId).orElseThrow(() -> new RecursoNoEncontradoException("Entrega", entregaId));
        e.reprogramar(reloj.instant());
        entregas.guardar(e);
        eventos.publicar(new EventosLogistica.EntregaReprogramada(e.id(), e.donacionId(), reloj.instant()));
        return e;
    }
}
