package ar.utn.donatrack.donaciones.beneficiarias.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EntidadRepositorio {

    Optional<EntidadBeneficiaria> buscar(UUID id);

    Map<UUID, EntidadBeneficiaria> buscarTodas(Collection<UUID> ids);

    boolean existeEmail(String email);

    void guardar(EntidadBeneficiaria entidad);

    List<EntidadBeneficiaria> listarActivas(String texto);
}
