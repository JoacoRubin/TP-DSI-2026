package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.ResumenEntregas;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Las métricas públicas se calculan con agregación sobre TODO el período, no sobre una muestra. */
@DataMongoTest(properties = "de.flapdoodle.mongodb.embedded.database-dir=target/mongo-test")
@Import({RepositorioEntregasMongo.class, IndicesMongo.class})
class ResumenPublicoMongoTest {

    @Autowired
    MongoTemplate mongo;
    @Autowired
    RepositorioEntregasMongo repositorio;

    private final Instant hace10Dias = Instant.parse("2026-11-14T12:00:00Z");
    private final Instant hace60Dias = Instant.parse("2026-09-25T12:00:00Z");

    private Entrega nueva(UUID entidadId, String nombre, Instant cuando) {
        return Entrega.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Ana", entidadId,
                new Destino(nombre, "x", -35, -58), new DetalleDonacion("Sillas", "Sillas", 1, "u"), 1, 1, cuando);
    }

    private void entregada(UUID entidadId, String nombre, Instant cuando) {
        Entrega e = nueva(entidadId, nombre, cuando);
        e.enviarAPlanificar(cuando);
        e.planificar(UUID.randomUUID(), Camion.registrar(UUID.randomUUID(), "AE123BC", 20, 3, 3500), cuando);
        e.iniciarTraslado(cuando);
        e.confirmarRecepcion(List.of(), cuando);
        repositorio.guardar(e);
    }

    @BeforeEach
    void preparar() {
        mongo.dropCollection(Colecciones.ENTREGAS);
    }

    @Test
    void cuentaTodasLasEntregadasDelPeriodoAunqueSeanMasDeLasQueSeMuestran() {
        UUID escuela = UUID.randomUUID();
        for (int i = 0; i < 250; i++) {
            entregada(escuela, "Escuela 12", hace10Dias);
        }
        assertThat(repositorio.resumenEntregadasDesde(Instant.parse("2026-11-01T00:00:00Z")))
                .isEqualTo(new ResumenEntregas(250, 1));
    }

    @Test
    void cuentaEntidadesPorIdentidadNoPorNombreYRespetaElPeriodo() {
        entregada(UUID.randomUUID(), "Comedor Esperanza", hace10Dias);
        entregada(UUID.randomUUID(), "Comedor Esperanza", hace10Dias); // homónima: es OTRA entidad
        entregada(UUID.randomUUID(), "Hogar", hace60Dias);             // fuera del período
        // una pendiente no cuenta
        repositorio.guardar(nueva(UUID.randomUUID(), "Pendiente", hace10Dias));

        assertThat(repositorio.resumenEntregadasDesde(Instant.parse("2026-11-01T00:00:00Z")))
                .isEqualTo(new ResumenEntregas(2, 2));
    }
}
