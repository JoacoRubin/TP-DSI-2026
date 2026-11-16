package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.EstadoRuta;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import ar.utn.donatrack.logistica.rutas.domain.RutaRepositorio;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacion;
import ar.utn.donatrack.logistica.rutas.domain.SolicitudPlanificacionRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Adaptadores ODM. El dominio se persiste TAL CUAL (sin anotaciones): Spring Data mapea
 * campos por convención y reconstruye con el único constructor de cada clase.
 */
final class Colecciones {
    static final String CAMIONES = "camiones";
    static final String ENTREGAS = "entregas";
    static final String RUTAS = "rutas";
    static final String SOLICITUDES = "solicitudes_planificacion";

    private Colecciones() {
    }
}

@Repository
class RepositorioCamionesMongo implements CamionRepositorio {

    private final MongoTemplate mongo;

    RepositorioCamionesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<Camion> buscar(UUID id) {
        return Optional.ofNullable(mongo.findById(id, Camion.class, Colecciones.CAMIONES));
    }

    public Optional<Camion> buscarPorPatente(String patente) {
        return Optional.ofNullable(mongo.findOne(Query.query(where("patente").is(patente)), Camion.class, Colecciones.CAMIONES));
    }

    public List<Camion> todos() {
        return mongo.find(new Query().with(Sort.by("patente")), Camion.class, Colecciones.CAMIONES);
    }

    public List<Camion> activos() {
        return mongo.find(Query.query(where("activo").is(true)).with(Sort.by("patente")), Camion.class, Colecciones.CAMIONES);
    }

    public void guardar(Camion camion) {
        mongo.save(camion, Colecciones.CAMIONES);
    }
}

@Repository
class RepositorioEntregasMongo implements EntregaRepositorio {

    private final MongoTemplate mongo;

    RepositorioEntregasMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<Entrega> buscar(UUID id) {
        return Optional.ofNullable(mongo.findById(id, Entrega.class, Colecciones.ENTREGAS));
    }

    public Optional<Entrega> buscarPorDonacion(UUID donacionId) {
        return Optional.ofNullable(mongo.findOne(Query.query(where("donacionId").is(donacionId))
                .with(Sort.by(Sort.Direction.DESC, "creadaEn")), Entrega.class, Colecciones.ENTREGAS));
    }

    public List<Entrega> buscarTodas(Collection<UUID> ids) {
        return mongo.find(Query.query(where("_id").in(ids)), Entrega.class, Colecciones.ENTREGAS);
    }

    public List<Entrega> enEstado(EstadoEntrega estado) {
        return mongo.find(Query.query(where("estado").is(estado)).with(Sort.by("creadaEn")), Entrega.class, Colecciones.ENTREGAS);
    }

    public List<Entrega> deRuta(UUID rutaId) {
        return mongo.find(Query.query(where("rutaId").is(rutaId)), Entrega.class, Colecciones.ENTREGAS);
    }

    public List<Entrega> filtrar(EstadoEntrega estado, UUID entidadId, UUID donanteId) {
        Criteria c = new Criteria();
        if (estado != null) c = c.and("estado").is(estado);
        if (entidadId != null) c = c.and("entidadId").is(entidadId);
        if (donanteId != null) c = c.and("donanteId").is(donanteId);
        return mongo.find(Query.query(c).with(Sort.by(Sort.Direction.DESC, "creadaEn")).limit(500), Entrega.class, Colecciones.ENTREGAS);
    }

    public List<Entrega> entregadasDesde(Instant desde, int limite) {
        return mongo.find(Query.query(where("estado").is(EstadoEntrega.ENTREGADA).and("entregadaEn").gte(desde))
                .with(Sort.by(Sort.Direction.DESC, "entregadaEn")).limit(limite), Entrega.class, Colecciones.ENTREGAS);
    }

    public void guardar(Entrega entrega) {
        mongo.save(entrega, Colecciones.ENTREGAS);
    }

    public void guardarTodas(Collection<Entrega> entregas) {
        entregas.forEach(this::guardar);
    }
}

@Repository
class RepositorioRutasMongo implements RutaRepositorio {

    private final MongoTemplate mongo;

    RepositorioRutasMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<Ruta> buscar(UUID id) {
        return Optional.ofNullable(mongo.findById(id, Ruta.class, Colecciones.RUTAS));
    }

    public void guardar(Ruta ruta) {
        mongo.save(ruta, Colecciones.RUTAS);
    }

    /**
     * $set de la última posición + $push con $slice: una sola operación atómica en el servidor.
     * No pisa cambios concurrentes de estado (ej. el chofer reporta mientras se finaliza la ruta).
     */
    public void guardarPosicion(Ruta ruta) {
        Update update = new Update().set("ultimaPosicion", ruta.ultimaPosicion());
        update.push("recorrido").slice(-Ruta.MAX_RECORRIDO).each(ruta.ultimaPosicion());
        mongo.updateFirst(Query.query(where("_id").is(ruta.id())), update, Colecciones.RUTAS);
    }

    public List<Ruta> enEstados(List<EstadoRuta> estados) {
        return mongo.find(Query.query(where("estado").in(estados)).with(Sort.by("fecha", "patente")), Ruta.class, Colecciones.RUTAS);
    }

    /** Consulta sobre el documento embebido: rutas en curso que llevan alguna donación del donante. */
    public List<Ruta> activasDeDonante(UUID donanteId) {
        return mongo.find(Query.query(where("estado").is(EstadoRuta.EN_CURSO).and("paradas.entregas.donanteId").is(donanteId)),
                Ruta.class, Colecciones.RUTAS);
    }

    public List<Ruta> activasDeEntidad(UUID entidadId) {
        return mongo.find(Query.query(where("estado").is(EstadoRuta.EN_CURSO).and("paradas.entidadId").is(entidadId)),
                Ruta.class, Colecciones.RUTAS);
    }

    public List<Ruta> dePatente(String patente, List<EstadoRuta> estados) {
        return mongo.find(Query.query(where("patente").is(patente).and("estado").in(estados)).with(Sort.by("fecha")),
                Ruta.class, Colecciones.RUTAS);
    }

    public List<Ruta> recientes(int limite) {
        return mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "creadaEn")).limit(limite), Ruta.class, Colecciones.RUTAS);
    }
}

@Repository
class RepositorioSolicitudesMongo implements SolicitudPlanificacionRepositorio {

    private final MongoTemplate mongo;

    RepositorioSolicitudesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<SolicitudPlanificacion> buscar(UUID id) {
        return Optional.ofNullable(mongo.findById(id, SolicitudPlanificacion.class, Colecciones.SOLICITUDES));
    }

    public void guardar(SolicitudPlanificacion s) {
        mongo.save(s, Colecciones.SOLICITUDES);
    }

    public List<SolicitudPlanificacion> recientes(int limite) {
        return mongo.find(new Query().with(Sort.by(Sort.Direction.DESC, "enviadaEn")).limit(limite),
                SolicitudPlanificacion.class, Colecciones.SOLICITUDES);
    }
}
