package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices declarados en infraestructura (no con anotaciones en el dominio). Pensados para las
 * consultas reales: estados, búsqueda por donación y seguimiento por donante/entidad.
 */
@Component
class IndicesMongo {

    private final MongoTemplate mongo;

    IndicesMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void crear() {
        mongo.indexOps(Colecciones.CAMIONES).ensureIndex(new Index("patente", Sort.Direction.ASC).unique());
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("donacionId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("creadaEn", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("rutaId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.ENTREGAS).ensureIndex(new Index("entidadId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("paradas.entregas.donanteId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("estado", Sort.Direction.ASC).on("paradas.entidadId", Sort.Direction.ASC));
        mongo.indexOps(Colecciones.RUTAS).ensureIndex(new Index("patente", Sort.Direction.ASC).on("estado", Sort.Direction.ASC));
    }
}
