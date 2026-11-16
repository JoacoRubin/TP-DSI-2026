package ar.utn.donatrack.donaciones.asignacion.domain;

import java.util.Optional;
import java.util.UUID;

public interface ResultadoMatchmakingRepositorio {

    /** Reemplaza el resultado anterior de la misma donación (sólo interesa la última ejecución). */
    void guardar(ResultadoMatchmaking resultado);

    Optional<ResultadoMatchmaking> deDonacion(UUID donacionId);

    void eliminarDeDonacion(UUID donacionId);

    PesosSemanticos pesos();

    void guardarPesos(PesosSemanticos pesos);
}
