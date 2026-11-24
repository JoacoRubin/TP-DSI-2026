package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;

import java.util.Arrays;
import org.springframework.stereotype.Component;

/**
 * Índices declarados en infraestructura (no con anotaciones en el dominio). Pensados para las
 * consultas reales: estados, búsqueda por donación y seguimiento por donante/entidad.
 */
@Component
class IndicesMongo {

    static final String UNICA_ACTIVA_POR_DONACION = "ux_entrega_activa_por_donacion";

    private final MongoTemplate mongo;

    IndicesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void crear() {
        mongo.indexOps(Colecciones.CAMIONES).ensureIndex(new Index("patente", Sort.Direction.ASC).unique());
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("donacionId", Sort.Direction.ASC));
        // Idempotencia real de CrearEntrega: a lo sumo UNA entrega no cancelada por donación.
        // Parcial porque una donación devuelta al depósito puede reasignarse (nueva entrega).
        // Requiere MongoDB >= 6.0 ($in en partialFilterExpression).
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("donacionId", Sort.Direction.ASC)
                .named(UNICA_ACTIVA_POR_DONACION).unique()
                .partial(PartialIndexFilter.of(Criteria.where("estado").in(Arrays.stream(EstadoEntrega.values())
                        .filter(e -> e != EstadoEntrega.CANCELADA).map(Enum::name).toList()))));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("creadaEn", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("rutaId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("entidadId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("paradas.entregas.donanteId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("paradas.entidadId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("patente", Sort.Direction.ASC).on("estado", Sort.Direction.ASC));
    }
}
