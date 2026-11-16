package ar.utn.donatrack.incentivos.actividad.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActividadRepositorio {

    Optional<ActividadDonacion> buscar(UUID donacionId);

    List<ActividadDonacion> deDonante(UUID donanteId);

    void guardar(ActividadDonacion actividad);
}
