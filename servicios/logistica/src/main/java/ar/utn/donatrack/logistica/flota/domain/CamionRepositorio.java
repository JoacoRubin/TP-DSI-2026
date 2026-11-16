package ar.utn.donatrack.logistica.flota.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CamionRepositorio {

    Optional<Camion> buscar(UUID id);

    Optional<Camion> buscarPorPatente(String patente);

    List<Camion> todos();

    List<Camion> activos();

    void guardar(Camion camion);
}
