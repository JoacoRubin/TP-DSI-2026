package ar.utn.donatrack.incentivos.infrastructure.persistencia;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadDonacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.misiones.domain.CategoriaDonante;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import ar.utn.donatrack.incentivos.ranking.domain.RankingMensual;
import ar.utn.donatrack.incentivos.ranking.domain.RankingRepositorio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "perfiles_donantes")
class PerfilJpa {
    @Id
    UUID donanteId;
    String nombre;
    String email;
    String telefono;
    String whatsapp;
    @Enumerated(EnumType.STRING)
    MedioNotificacion medioPreferido;
    boolean personaHumana;
    @Enumerated(EnumType.STRING)
    CategoriaDonante categoria;
    int indiceMision;
    boolean insigniasVisibles;
    Instant creadoEn;

    protected PerfilJpa() {
    }

    static PerfilJpa desde(PerfilDonante p) {
        PerfilJpa j = new PerfilJpa();
        j.donanteId = p.donanteId();
        j.nombre = p.nombre();
        j.email = p.email();
        j.telefono = p.telefono();
        j.whatsapp = p.whatsapp();
        j.medioPreferido = p.medioPreferido();
        j.personaHumana = p.personaHumana();
        j.categoria = p.categoria();
        j.indiceMision = p.indiceMision();
        j.insigniasVisibles = p.insigniasVisibles();
        j.creadoEn = p.creadoEn();
        return j;
    }

    PerfilDonante aDominio() {
        return new PerfilDonante(donanteId, nombre, email, telefono, whatsapp, medioPreferido, personaHumana, categoria,
                indiceMision, insigniasVisibles, creadoEn);
    }
}

@Entity
@Table(name = "actividad_donaciones", indexes = @Index(name = "ix_actividad_donante", columnList = "donanteId,registradaEn"))
class ActividadJpa {
    @Id
    UUID donacionId;
    UUID donanteId;
    UUID cargaId;
    String categoria;
    String subcategoria;
    int cantidad;
    Instant registradaEn;
    boolean entregada;
    UUID entidadId;
    Instant entregadaEn;

    protected ActividadJpa() {
    }

    static ActividadJpa desde(ActividadDonacion a) {
        ActividadJpa j = new ActividadJpa();
        j.donacionId = a.donacionId();
        j.donanteId = a.donanteId();
        j.cargaId = a.cargaId();
        j.categoria = a.categoria();
        j.subcategoria = a.subcategoria();
        j.cantidad = a.cantidad();
        j.registradaEn = a.registradaEn();
        j.entregada = a.entregada();
        j.entidadId = a.entidadId();
        j.entregadaEn = a.entregadaEn();
        return j;
    }

    ActividadDonacion aDominio() {
        return new ActividadDonacion(donacionId, donanteId, cargaId, categoria, subcategoria, cantidad, registradaEn,
                entregada, entidadId, entregadaEn);
    }
}

@Entity
@Table(name = "insignias", indexes = {
        @Index(name = "ix_insignias_donante", columnList = "donanteId"),
        @Index(name = "ix_insignias_fecha", columnList = "obtenidaEn")})
class InsigniaJpa {
    @Id
    UUID id;
    UUID donanteId;
    String misionId;
    String nombre;
    String icono;
    @Enumerated(EnumType.STRING)
    CategoriaDonante categoria;
    Instant obtenidaEn;

    protected InsigniaJpa() {
    }

    static InsigniaJpa desde(InsigniaObtenida i) {
        InsigniaJpa j = new InsigniaJpa();
        j.id = i.id();
        j.donanteId = i.donanteId();
        j.misionId = i.misionId();
        j.nombre = i.nombre();
        j.icono = i.icono();
        j.categoria = i.categoria();
        j.obtenidaEn = i.obtenidaEn();
        return j;
    }

    InsigniaObtenida aDominio() {
        return new InsigniaObtenida(id, donanteId, misionId, nombre, icono, categoria, obtenidaEn);
    }
}

@Entity
@Table(name = "rankings_mensuales")
class RankingJpa {
    @Embeddable
    static class EntradaJpa {
        int posicion;
        UUID donanteId;
        String nombre;
        int misionesCumplidas;
    }

    @Id
    String id;
    int anio;
    int mes;
    Instant generadoEn;
    @ElementCollection
    @CollectionTable(name = "rankings_entradas", joinColumns = @JoinColumn(name = "ranking_id"))
    @OrderColumn(name = "orden")
    List<EntradaJpa> entradas = new ArrayList<>();

    protected RankingJpa() {
    }

    static RankingJpa desde(RankingMensual r) {
        RankingJpa j = new RankingJpa();
        j.id = r.id();
        j.anio = r.anio();
        j.mes = r.mes();
        j.generadoEn = r.generadoEn();
        r.entradas().forEach(e -> {
            EntradaJpa x = new EntradaJpa();
            x.posicion = e.posicion();
            x.donanteId = e.donanteId();
            x.nombre = e.nombre();
            x.misionesCumplidas = e.misionesCumplidas();
            j.entradas.add(x);
        });
        return j;
    }

    RankingMensual aDominio() {
        return new RankingMensual(id, anio, mes, entradas.stream()
                .map(e -> new RankingMensual.Entrada(e.posicion, e.donanteId, e.nombre, e.misionesCumplidas)).toList(), generadoEn);
    }
}

interface PerfilJpaRepository extends JpaRepository<PerfilJpa, UUID> {
}

interface ActividadJpaRepository extends JpaRepository<ActividadJpa, UUID> {
    List<ActividadJpa> findByDonanteIdOrderByRegistradaEnAsc(UUID donanteId);
}

interface InsigniaJpaRepository extends JpaRepository<InsigniaJpa, UUID> {
    List<InsigniaJpa> findByDonanteIdOrderByObtenidaEnAsc(UUID donanteId);

    List<InsigniaJpa> findByObtenidaEnGreaterThanEqualAndObtenidaEnLessThan(Instant desde, Instant hasta);
}

interface RankingJpaRepository extends JpaRepository<RankingJpa, String> {
    List<RankingJpa> findAllByOrderByAnioDescMesDesc();
}

@Repository
@Transactional
class RepositorioPerfiles implements Repositorios.PerfilRepositorio {
    private final PerfilJpaRepository jpa;

    RepositorioPerfiles(PerfilJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Transactional(readOnly = true)
    public Optional<PerfilDonante> buscar(UUID id) {
        return jpa.findById(id).map(PerfilJpa::aDominio);
    }

    @Transactional(readOnly = true)
    public List<PerfilDonante> buscarTodos(List<UUID> ids) {
        return jpa.findAllById(ids).stream().map(PerfilJpa::aDominio).toList();
    }

    public void guardar(PerfilDonante p) {
        jpa.save(PerfilJpa.desde(p));
    }
}

@Repository
@Transactional
class RepositorioActividades implements ActividadRepositorio {
    private final ActividadJpaRepository jpa;

    RepositorioActividades(ActividadJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Transactional(readOnly = true)
    public Optional<ActividadDonacion> buscar(UUID id) {
        return jpa.findById(id).map(ActividadJpa::aDominio);
    }

    @Transactional(readOnly = true)
    public List<ActividadDonacion> deDonante(UUID donanteId) {
        return jpa.findByDonanteIdOrderByRegistradaEnAsc(donanteId).stream().map(ActividadJpa::aDominio).toList();
    }

    public void guardar(ActividadDonacion a) {
        jpa.save(ActividadJpa.desde(a));
    }
}

@Repository
@Transactional
class RepositorioInsignias implements Repositorios.InsigniaRepositorio {
    private final InsigniaJpaRepository jpa;

    RepositorioInsignias(InsigniaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Transactional(readOnly = true)
    public List<InsigniaObtenida> deDonante(UUID donanteId) {
        return jpa.findByDonanteIdOrderByObtenidaEnAsc(donanteId).stream().map(InsigniaJpa::aDominio).toList();
    }

    @Transactional(readOnly = true)
    public List<InsigniaObtenida> obtenidasEntre(Instant desde, Instant hasta) {
        return jpa.findByObtenidaEnGreaterThanEqualAndObtenidaEnLessThan(desde, hasta).stream().map(InsigniaJpa::aDominio).toList();
    }

    public void guardarTodas(List<InsigniaObtenida> insignias) {
        jpa.saveAll(insignias.stream().map(InsigniaJpa::desde).toList());
    }
}

@Repository
@Transactional
class RepositorioRankings implements RankingRepositorio {
    private final RankingJpaRepository jpa;

    RepositorioRankings(RankingJpaRepository jpa) {
        this.jpa = jpa;
    }

    public void guardar(RankingMensual r) {
        jpa.findById(r.id()).ifPresent(jpa::delete);
        jpa.flush();
        jpa.save(RankingJpa.desde(r));
    }

    @Transactional(readOnly = true)
    public Optional<RankingMensual> buscar(String id) {
        return jpa.findById(id).map(RankingJpa::aDominio);
    }

    @Transactional(readOnly = true)
    public List<RankingMensual> historial() {
        return jpa.findAllByOrderByAnioDescMesDesc().stream().map(RankingJpa::aDominio).toList();
    }
}
