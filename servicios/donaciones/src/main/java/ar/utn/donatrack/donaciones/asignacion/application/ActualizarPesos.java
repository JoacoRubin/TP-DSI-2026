package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.donaciones.asignacion.domain.PesosSemanticos;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;

public class ActualizarPesos {

    private final ResultadoMatchmakingRepositorio repositorio;

    public ActualizarPesos(ResultadoMatchmakingRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public PesosSemanticos ejecutar(double textual, double volumen, double tipo) {
        PesosSemanticos pesos = new PesosSemanticos(textual, volumen, tipo);
        repositorio.guardarPesos(pesos);
        return pesos;
    }
}
