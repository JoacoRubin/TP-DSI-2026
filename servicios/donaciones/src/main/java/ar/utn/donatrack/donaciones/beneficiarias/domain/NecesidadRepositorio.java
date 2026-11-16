package ar.utn.donatrack.donaciones.beneficiarias.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NecesidadRepositorio {

    Optional<Necesidad> buscar(UUID id);

    void guardar(Necesidad necesidad);

    List<Necesidad> deEntidad(UUID entidadId);

    /** Necesidades activas de la subcategoría (candidatas al matchmaking). */
    List<Necesidad> activasDeSubcategoria(UUID subcategoriaId);
}
