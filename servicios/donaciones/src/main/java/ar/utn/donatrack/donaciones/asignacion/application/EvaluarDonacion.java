package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;

import java.util.UUID;

/** Recalcula el ranking de una única donación (a demanda desde la pantalla de asignación). */
public class EvaluarDonacion {

    private final EvaluadorDonaciones evaluador;
    private final DonacionRepositorio donaciones;

    public EvaluarDonacion(EvaluadorDonaciones evaluador, DonacionRepositorio donaciones) {
        this.evaluador = evaluador;
        this.donaciones = donaciones;
    }

    public ResultadoMatchmaking ejecutar(UUID donacionId) {
        Donacion d = donaciones.buscar(donacionId).orElseThrow(() -> new RecursoNoEncontradoException("Donación", donacionId));
        if (d.estado() != EstadoDonacion.EN_DEPOSITO) {
            throw new ReglaDeNegocioException("Sólo se evalúan donaciones 'En depósito'");
        }
        return evaluador.evaluar(d, evaluador.contexto());
    }
}
