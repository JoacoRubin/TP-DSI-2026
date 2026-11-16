package ar.utn.donatrack.donaciones.donaciones.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.FiltroDonaciones;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

interface DonacionJpaRepository extends JpaRepository<DonacionJpa, UUID>, JpaSpecificationExecutor<DonacionJpa> {

    List<DonacionJpa> findByEstadoOrderByRegistradaEnAsc(EstadoDonacion estado);

    @Query("select d.entidadId, count(d) from DonacionJpa d where d.estado = :estado and d.entregadaEn >= :desde group by d.entidadId")
    List<Object[]> contarPorEntidad(@Param("estado") EstadoDonacion estado, @Param("desde") Instant desde);

    List<DonacionJpa> findByEstadoAndEntregadaEnAfterOrderByEntregadaEnDesc(EstadoDonacion estado, Instant desde, Pageable p);
}

@Repository
@Transactional
class RepositorioDonacionesJpa implements DonacionRepositorio {

    private final DonacionJpaRepository jpa;

    RepositorioDonacionesJpa(DonacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Donacion> buscar(UUID id) {
        return jpa.findById(id).map(DonacionJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donacion> buscarTodas(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(DonacionJpa::aDominio).toList();
    }

    @Override
    public void guardar(Donacion donacion) {
        jpa.save(DonacionJpa.desde(donacion));
    }

    @Override
    public void guardarTodas(Collection<Donacion> donaciones) {
        jpa.saveAll(donaciones.stream().map(DonacionJpa::desde).toList());
    }

    @Override
    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Donacion> buscar(FiltroDonaciones f, int pagina, int tamanio) {
        Specification<DonacionJpa> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.donanteId() != null) p.add(cb.equal(root.get("donanteId"), f.donanteId()));
            if (f.entidadId() != null) p.add(cb.equal(root.get("entidadId"), f.entidadId()));
            if (f.estado() != null) p.add(cb.equal(root.get("estado"), f.estado()));
            if (f.categoria() != null && !f.categoria().isBlank()) p.add(cb.equal(root.get("categoriaNombre"), f.categoria()));
            if (f.subcategoriaId() != null) p.add(cb.equal(root.get("subcategoriaId"), f.subcategoriaId()));
            return cb.and(p.toArray(Predicate[]::new));
        };
        Page<DonacionJpa> r = jpa.findAll(spec, PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "registradaEn")));
        return new Pagina<>(r.getContent().stream().map(DonacionJpa::aDominio).toList(), pagina, tamanio, r.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donacion> enEstado(EstadoDonacion estado) {
        return jpa.findByEstadoOrderByRegistradaEnAsc(estado).stream().map(DonacionJpa::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> entregadasPorEntidadDesde(Instant desde) {
        Map<UUID, Integer> r = new HashMap<>();
        for (Object[] fila : jpa.contarPorEntidad(EstadoDonacion.ENTREGADA, desde)) {
            r.put((UUID) fila[0], ((Number) fila[1]).intValue());
        }
        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donacion> entregadasDesde(Instant desde, int limite) {
        return jpa.findByEstadoAndEntregadaEnAfterOrderByEntregadaEnDesc(EstadoDonacion.ENTREGADA, desde, PageRequest.of(0, limite))
                .stream().map(DonacionJpa::aDominio).toList();
    }
}
