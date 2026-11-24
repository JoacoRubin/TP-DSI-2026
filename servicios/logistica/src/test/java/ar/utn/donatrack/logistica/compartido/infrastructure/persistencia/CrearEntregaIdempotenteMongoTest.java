package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import ar.utn.donatrack.logistica.entregas.application.CrearEntrega;
import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/** Regresión: dos avisos concurrentes de la misma donación no deben crear dos entregas activas. */
@DataMongoTest(properties = "de.flapdoodle.mongodb.embedded.database-dir=target/mongo-test")
@Import({RepositorioEntregasMongo.class, IndicesMongo.class})
class CrearEntregaIdempotenteMongoTest {

    @Autowired
    MongoTemplate mongo;
    @Autowired
    RepositorioEntregasMongo repositorio;
    @Autowired
    IndicesMongo indices;

    private final Instant ahora = Instant.parse("2026-11-24T12:00:00Z");

    @BeforeEach
    void preparar() {
        mongo.dropCollection(Colecciones.ENTREGAS);
        indices.crear();
    }

    private CrearEntrega.Comando comando(UUID donacionId) {
        return new CrearEntrega.Comando(donacionId, UUID.randomUUID(), "Ana", UUID.randomUUID(), "Escuela 12",
                "Ruta 6 km 32", -35.05, -58.76, "6 unidades de Sillas", "Sillas", 6, "unidades", 30, 1.2);
    }

    private Entrega nueva(UUID donacionId) {
        return Entrega.nueva(UUID.randomUUID(), donacionId, UUID.randomUUID(), "Ana", UUID.randomUUID(),
                new Destino("Escuela", "Ruta 6 km 32", -35.05, -58.76),
                new DetalleDonacion("6 unidades de Sillas", "Sillas", 6, "unidades"), 30, 1.2, ahora);
    }

    private long entregasDe(UUID donacionId) {
        return mongo.count(Query.query(where("donacionId").is(donacionId)), Entrega.class, Colecciones.ENTREGAS);
    }

    @Test
    void avisosConcurrentesDeLaMismaDonacionCreanUnaSolaEntrega() throws Exception {
        UUID donacionId = UUID.randomUUID();
        CrearEntrega crear = new CrearEntrega(repositorio, Clock.systemUTC());
        int hilos = 8;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<Entrega>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            Callable<Entrega> tarea = () -> {
                largada.await();
                return crear.ejecutar(comando(donacionId));
            };
            resultados.add(pool.submit(tarea));
        }
        largada.countDown();
        List<UUID> ids = new ArrayList<>();
        for (Future<Entrega> f : resultados) {
            ids.add(f.get().id());
        }
        pool.shutdown();

        assertThat(entregasDe(donacionId)).isEqualTo(1);
        assertThat(ids).containsOnly(ids.get(0));
    }

    @Test
    void unaEntregaCanceladaNoImpideCrearOtraParaLaMismaDonacion() {
        UUID donacionId = UUID.randomUUID();
        Entrega primera = repositorio.registrarSiNoHayActiva(nueva(donacionId));
        primera.cancelar("Devuelta al depósito", ahora);
        repositorio.guardar(primera);

        Entrega segunda = repositorio.registrarSiNoHayActiva(nueva(donacionId));

        assertThat(segunda.id()).isNotEqualTo(primera.id());
        assertThat(segunda.estado()).isNotEqualTo(EstadoEntrega.CANCELADA);
        assertThat(entregasDe(donacionId)).isEqualTo(2);
    }
}
