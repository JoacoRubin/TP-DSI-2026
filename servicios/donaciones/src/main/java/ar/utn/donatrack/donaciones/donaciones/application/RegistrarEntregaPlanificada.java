package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;

import java.time.Clock;

/** Reacción al evento de Logística: la donación quedó incluida en una ruta. Idempotente. */
public class RegistrarEntregaPlanificada {

    private final DonacionRepositorio donaciones;
    private final Clock reloj;

    public RegistrarEntregaPlanificada(DonacionRepositorio donaciones, Clock reloj) {
        this.donaciones = donaciones;
        this.reloj = reloj;
    }

    public void ejecutar(EventosLogistica.EntregaPlanificada evento) {
        donaciones.buscar(evento.donacionId())
                .filter(d -> d.estado() == EstadoDonacion.ASIGNADA || d.estado() == EstadoDonacion.ENTREGA_FALLIDA)
                .ifPresent(d -> {
                    d.marcarListaParaEntregar(reloj.instant());
                    donaciones.guardar(d);
                });
    }
}
