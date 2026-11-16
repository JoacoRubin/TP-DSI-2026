package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;

import java.util.UUID;

/** Sólo para corregir cargas erróneas: una donación con historia no se borra, se audita. */
public class EliminarDonacion {

    private final DonacionRepositorio donaciones;
    private final ResultadoMatchmakingRepositorio resultados;

    public EliminarDonacion(DonacionRepositorio donaciones, ResultadoMatchmakingRepositorio resultados) {
        this.donaciones = donaciones;
        this.resultados = resultados;
    }

    public void ejecutar(UUID id) {
        Donacion d = donaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donación", id));
        if (!d.puedeEliminarse()) {
            throw new ReglaDeNegocioException("Sólo pueden eliminarse donaciones que siguen 'En depósito' sin asignar");
        }
        resultados.eliminarDeDonacion(id);
        donaciones.eliminar(id);
    }
}
