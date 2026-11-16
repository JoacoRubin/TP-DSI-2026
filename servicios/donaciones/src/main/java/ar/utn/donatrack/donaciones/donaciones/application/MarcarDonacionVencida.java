package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;

import java.time.Clock;
import java.util.UUID;

/**
 * La administración del depósito marca la donación como vencida. Si estaba asignada se libera
 * lo comprometido en la necesidad y se cancela la entrega pendiente en Logística.
 */
public class MarcarDonacionVencida {

    private final DonacionRepositorio donaciones;
    private final NecesidadRepositorio necesidades;
    private final ResultadoMatchmakingRepositorio resultados;
    private final ServicioLogistica logistica;
    private final Clock reloj;

    public MarcarDonacionVencida(DonacionRepositorio donaciones, NecesidadRepositorio necesidades,
                                 ResultadoMatchmakingRepositorio resultados, ServicioLogistica logistica, Clock reloj) {
        this.donaciones = donaciones;
        this.necesidades = necesidades;
        this.resultados = resultados;
        this.logistica = logistica;
        this.reloj = reloj;
    }

    public Donacion ejecutar(UUID id, String motivo, Solicitante solicitante) {
        Donacion d = donaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donación", id));
        EstadoDonacion anterior = d.estado();
        d.marcarVencida(solicitante.usuario(), motivo, reloj.instant());
        if (d.necesidadId() != null) {
            necesidades.buscar(d.necesidadId()).ifPresent(n -> {
                n.liberar(d.cantidadTotal());
                necesidades.guardar(n);
            });
        }
        donaciones.guardar(d);
        resultados.eliminarDeDonacion(id);
        if (anterior != EstadoDonacion.EN_DEPOSITO) {
            logistica.cancelarEntrega(id);
        }
        return d;
    }
}
