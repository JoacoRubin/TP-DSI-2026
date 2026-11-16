package ar.utn.donatrack.donaciones.asignacion.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Información del "mundo" que algunos algoritmos necesitan, sin acoplarlos a repositorios. */
public interface ContextoMatchmaking {

    LocalDate hoy();

    int donacionesRecibidasEnTrimestre(UUID entidadId);
}
