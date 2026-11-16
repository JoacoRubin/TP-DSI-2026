package ar.utn.donatrack.donaciones.asignacion.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de evaluar UNA donación: un ranking por algoritmo y las coincidencias (necesidades
 * presentes en el top de TODOS los algoritmos), que es lo que la administración confirma.
 * Si no hay coincidencias, la interfaz muestra los rankings por separado.
 */
public record ResultadoMatchmaking(UUID donacionId, Instant ejecutadoEn, List<Ranking> rankings,
                                   List<UUID> coincidencias) {

    public ResultadoMatchmaking {
        rankings = List.copyOf(rankings);
        coincidencias = List.copyOf(coincidencias);
    }

    public boolean hayCoincidencias() {
        return !coincidencias.isEmpty();
    }

    public boolean propone(UUID necesidadId) {
        return rankings.stream().anyMatch(r -> r.contiene(necesidadId));
    }
}
