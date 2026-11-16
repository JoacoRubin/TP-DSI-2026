package ar.utn.donatrack.logistica.rutas.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudPlanificacionRepositorio {

    Optional<SolicitudPlanificacion> buscar(UUID id);

    void guardar(SolicitudPlanificacion solicitud);

    List<SolicitudPlanificacion> recientes(int limite);
}
