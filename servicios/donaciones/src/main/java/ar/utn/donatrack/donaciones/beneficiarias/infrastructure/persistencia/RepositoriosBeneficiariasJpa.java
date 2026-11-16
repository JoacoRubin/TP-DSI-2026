package ar.utn.donatrack.donaciones.beneficiarias.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

interface EntidadJpaRepository extends JpaRepository<EntidadJpa, UUID> {

    boolean existsByEmailPrincipal(String email);

    @Query("""
            select e from EntidadJpa e
            where e.activa = true and (:texto is null or lower(e.razonSocial) like lower(concat('%', :texto, '%')))
            order by e.razonSocial""")
    List<EntidadJpa> activas(@Param("texto") String texto);
}

interface NecesidadJpaRepository extends JpaRepository<NecesidadJpa, UUID> {

    List<NecesidadJpa> findByEntidadIdOrderByCreadaEnDesc(UUID entidadId);

    List<NecesidadJpa> findByActivaTrueAndSubcategoriaId(UUID subcategoriaId);
}

@Repository
@Transactional
class RepositorioEntidadesJpa implements EntidadRepositorio {

    private final EntidadJpaRepository jpa;

    RepositorioEntidadesJpa(EntidadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntidadBeneficiaria> buscar(UUID id) {
        return jpa.findById(id).map(EntidadJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, EntidadBeneficiaria> buscarTodas(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(EntidadJpa::aDominio)
                .collect(Collectors.toMap(EntidadBeneficiaria::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return jpa.existsByEmailPrincipal(email);
    }

    @Override
    public void guardar(EntidadBeneficiaria entidad) {
        jpa.save(EntidadJpa.desde(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntidadBeneficiaria> listarActivas(String texto) {
        return jpa.activas(texto == null || texto.isBlank() ? null : texto.trim()).stream().map(EntidadJpa::aDominio).toList();
    }
}

@Repository
@Transactional
class RepositorioNecesidadesJpa implements NecesidadRepositorio {

    private final NecesidadJpaRepository jpa;

    RepositorioNecesidadesJpa(NecesidadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Necesidad> buscar(UUID id) {
        return jpa.findById(id).map(NecesidadJpa::aDominio);
    }

    @Override
    public void guardar(Necesidad necesidad) {
        jpa.save(NecesidadJpa.desde(necesidad));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Necesidad> deEntidad(UUID entidadId) {
        return jpa.findByEntidadIdOrderByCreadaEnDesc(entidadId).stream().map(NecesidadJpa::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Necesidad> activasDeSubcategoria(UUID subcategoriaId) {
        return jpa.findByActivaTrueAndSubcategoriaId(subcategoriaId).stream().map(NecesidadJpa::aDominio).toList();
    }
}
