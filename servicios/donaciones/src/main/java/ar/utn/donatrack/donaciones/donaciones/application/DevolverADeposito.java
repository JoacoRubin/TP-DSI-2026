package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;

import java.time.Clock;
import java.util.UUID;

/**
 * Tras revisar una entrega fallida (o deshacer una asignación), la administración decide que
 * la donación vuelva a estar disponible para un nuevo matchmaking.
 */
public class DevolverADeposito {

    private final DonacionRepositorio donaciones;
    private final NecesidadRepositorio necesidades;
    private final ServicioLogistica logistica;
    private final Clock reloj;

    public DevolverADeposito(DonacionRepositorio donaciones, NecesidadRepositorio necesidades,
                             ServicioLogistica logistica, Clock reloj) {
        this.donaciones = donaciones;
        this.necesidades = necesidades;
        this.logistica = logistica;
        this.reloj = reloj;
    }

    public Donacion ejecutar(UUID id, String motivo, Solicitante solicitante) {
        Donacion d = donaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donación", id));
        UUID necesidadId = d.necesidadId();
        d.devolverADeposito(solicitante.usuario(), motivo == null ? "Reasignación" : motivo, reloj.instant());
        if (necesidadId != null) {
            necesidades.buscar(necesidadId).ifPresent(n -> {
                n.liberar(d.cantidadTotal());
                necesidades.guardar(n);
            });
        }
        donaciones.guardar(d);
        logistica.cancelarEntrega(id);
        return d;
    }
}
