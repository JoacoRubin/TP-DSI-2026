package ar.utn.donatrack.donaciones.importacion.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacionRepositorio {

    void guardar(Importacion importacion);

    Optional<Importacion> buscar(UUID id);

    List<Importacion> recientes(int limite);

    List<Importacion> enCurso();
}
