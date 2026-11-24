package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.entregas.domain.FiltroEntregas;
import ar.utn.donatrack.logistica.entregas.domain.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Filtros dinámicos que expone GraphQL, resueltos en la base (no en memoria). */
@DataMongoTest(properties = "de.flapdoodle.mongodb.embedded.database-dir=target/mongo-test")
@Import({RepositorioEntregasMongo.class, IndicesMongo.class})
class BuscarEntregasMongoTest {

    @Autowired
    MongoTemplate mongo;
    @Autowired
    RepositorioEntregasMongo repositorio;

    private final UUID escuela = UUID.randomUUID();
    private final Instant dia1 = Instant.parse("2026-11-01T12:00:00Z");
    private final Instant dia10 = Instant.parse("2026-11-10T12:00:00Z");

    private Entrega entrega(UUID entidadId, String entidad, String descripcion, Instant creada) {
        Entrega e = Entrega.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Ana", entidadId,
                new Destino(entidad, "Ruta 6 km 32", -35.05, -58.76),
                new DetalleDonacion(descripcion, "Sillas", 6, "unidades"), 30, 1.2, creada);
        repositorio.guardar(e);
        return e;
    }

    @BeforeEach
    void preparar() {
        mongo.dropCollection(Colecciones.ENTREGAS);
        entrega(escuela, "Escuela 12", "6 unidades de Sillas", dia1);
        entrega(escuela, "Escuela 12", "Mesas (2)", dia10);
        Entrega cancelada = entrega(escuela, "Escuela 12", "Camperas", dia10);
        cancelada.cancelar("prueba", dia10);
        repositorio.guardar(cancelada);
        entrega(UUID.randomUUID(), "Comedor Los Pibes", "Fideos secos", dia10);
    }

    @Test
    void combinaEstadosEntidadYFechas() {
        Pagina<Entrega> p = repositorio.buscar(new FiltroEntregas(Set.of(EstadoEntrega.PENDIENTE), escuela, null, null,
                Instant.parse("2026-11-05T00:00:00Z"), null, null), 0, 20);

        assertThat(p.total()).isEqualTo(1);
        assertThat(p.elementos()).extracting(e -> e.detalle().descripcion()).containsExactly("Mesas (2)");
    }

    @Test
    void buscaTextoSinDistinguirMayusculasYSinInterpretarRegex() {
        assertThat(repositorio.buscar(FiltroEntregas.conTexto("comedor"), 0, 20).total()).isEqualTo(1);
        assertThat(repositorio.buscar(FiltroEntregas.conTexto("sillas"), 0, 20).total()).isEqualTo(1);
        // "(" es metacaracter de regex: debe buscarse literal, no romper ni matchear todo
        assertThat(repositorio.buscar(FiltroEntregas.conTexto("(2)"), 0, 20).total()).isEqualTo(1);
        assertThat(repositorio.buscar(FiltroEntregas.conTexto(".*"), 0, 20).total()).isZero();
    }

    @Test
    void traeSoloLasEntregasDeLasDonacionesPedidasLaMasNuevaPrimero() {
        UUID donacion = UUID.randomUUID();
        Entrega vieja = Entrega.nueva(UUID.randomUUID(), donacion, UUID.randomUUID(), "Ana", escuela,
                new Destino("Escuela 12", "x", -35, -58), new DetalleDonacion("Sillas", "Sillas", 1, "u"), 1, 1, dia1);
        vieja.cancelar("devuelta", dia1);
        repositorio.guardar(vieja);
        Entrega nueva = Entrega.nueva(UUID.randomUUID(), donacion, UUID.randomUUID(), "Ana", escuela,
                new Destino("Escuela 12", "x", -35, -58), new DetalleDonacion("Sillas", "Sillas", 1, "u"), 1, 1, dia10);
        repositorio.guardar(nueva);

        assertThat(repositorio.deDonaciones(Set.of(donacion))).extracting(Entrega::id).containsExactly(nueva.id(), vieja.id());
        assertThat(repositorio.deDonaciones(Set.of())).isEmpty();
    }

    @Test
    void paginaConTotalRealOrdenandoDeLaMasNueva() {
        Pagina<Entrega> primera = repositorio.buscar(FiltroEntregas.sinFiltros(), 0, 3);
        Pagina<Entrega> segunda = repositorio.buscar(FiltroEntregas.sinFiltros(), 1, 3);

        assertThat(primera.total()).isEqualTo(4);
        assertThat(primera.elementos()).hasSize(3);
        assertThat(segunda.elementos()).hasSize(1);
        assertThat(segunda.elementos().get(0).creadaEn()).isEqualTo(dia1);
    }
}
