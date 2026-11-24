package ar.utn.donatrack.donaciones.donantes.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DonanteJpaRepository extends JpaRepository<DonanteJpa, UUID> {

    Optional<DonanteJpa> findByEmail(String email);

    List<DonanteJpa> findByEmailIn(Collection<String> emails);

    boolean existsByEmail(String email);

    List<DonanteJpa> findByActivoTrueAndUltimaInteraccionBefore(Instant limite);

    @Query("""
            select d from DonanteJpa d
            where :texto is null
               or lower(d.nombreVisible) like lower(concat('%', :texto, '%'))
               or lower(d.email) like lower(concat('%', :texto, '%'))
               or d.numeroDocumento like concat('%', :texto, '%')
            order by d.nombreVisible""")
    Page<DonanteJpa> buscar(@Param("texto") String texto, Pageable pagina);
}

/** Adaptador: el mapeo a dominio ocurre DENTRO de la transacción (colecciones lazy). */
@Repository
@Transactional
class RepositorioDonantesJpa implements DonanteRepositorio {

    private final DonanteJpaRepository jpa;

    RepositorioDonantesJpa(DonanteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Donante> buscar(UUID id) {
        return jpa.findById(id).map(DonanteJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Donante> buscarPorEmail(String email) {
        return jpa.findByEmail(email).map(DonanteJpa::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donante> buscarPorEmails(Collection<String> emails) {
        return emails.isEmpty() ? List.of() : jpa.findByEmailIn(emails).stream().map(DonanteJpa::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public void guardar(Donante donante) {
        // Flush inmediato: las altas crean después la cuenta remota (no idempotente); un error
        // local debe aparecer ACÁ y no en el commit, cuando la cuenta ya existe.
        jpa.saveAndFlush(DonanteJpa.desde(donante));
    }

    @Override
    public void guardarTodos(Collection<? extends Donante> donantes) {
        // Precarga en una sola consulta: los merge posteriores se resuelven contra el contexto de persistencia.
        jpa.findAllById(donantes.stream().map(Donante::id).toList());
        jpa.saveAll(donantes.stream().map(DonanteJpa::desde).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Donante> listar(String texto, int pagina, int tamanio) {
        Page<DonanteJpa> p = jpa.buscar(texto == null || texto.isBlank() ? null : texto.trim(), PageRequest.of(pagina, tamanio));
        return new Pagina<>(p.getContent().stream().map(DonanteJpa::aDominio).toList(), pagina, tamanio, p.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donante> sinInteraccionDesde(Instant limite) {
        return jpa.findByActivoTrueAndUltimaInteraccionBefore(limite).stream().map(DonanteJpa::aDominio).toList();
    }
}
