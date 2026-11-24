package ar.utn.donatrack.logistica.compartido.infrastructure.persistencia;

import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.rutas.domain.Parada;
import ar.utn.donatrack.logistica.rutas.domain.Posicion;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo de auditoría: dos reportes GPS simultáneos leen la misma ruta, ambos pasan el chequeo
 * "no aceptar posiciones viejas" en memoria, y si el más viejo se escribe último el camión "retrocede".
 * Se reproduce el resultado de esa carrera: dos copias de la ruta escriben en orden invertido.
 */
@DataMongoTest(properties = "de.flapdoodle.mongodb.embedded.database-dir=target/mongo-test-rutas") // otro contexto => otro mongod: no compartir directorio
@Import(RepositorioRutasMongo.class)
class PosicionConcurrenteMongoTest {

    @Autowired
    RepositorioRutasMongo rutas;

    private final Instant inicio = Instant.parse("2026-11-24T12:00:00Z");

    private Ruta rutaIniciada() {
        Entrega e = Entrega.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Ana", UUID.randomUUID(),
                new Destino("Escuela", "x", -35, -58), new DetalleDonacion("Sillas", "Sillas", 1, "u"), 1, 1, inicio);
        Ruta r = Ruta.planificar(UUID.randomUUID(), LocalDate.of(2026, 11, 24),
                Camion.registrar(UUID.randomUUID(), "AE123BC", 20, 3, 3500),
                List.of(Parada.de(1, e.entidadId(), e.destino(), List.of(e))), inicio);
        r.iniciar(inicio);
        rutas.guardar(r);
        return r;
    }

    @Test
    void unaPosicionViejaQueSeEscribeTardeNoPisaLaMasNueva() {
        Ruta guardada = rutaIniciada();
        // Dos requests concurrentes: cada una leyó la ruta SIN posición todavía.
        Ruta lecturaA = rutas.buscar(guardada.id()).orElseThrow();
        Ruta lecturaB = rutas.buscar(guardada.id()).orElseThrow();
        Posicion nueva = new Posicion(-34.60, -58.40, 40, inicio.plusSeconds(20));
        Posicion vieja = new Posicion(-34.70, -58.50, 40, inicio.plusSeconds(10));
        lecturaA.registrarPosicion(nueva, inicio.plusSeconds(21));
        lecturaB.registrarPosicion(vieja, inicio.plusSeconds(21));

        rutas.guardarPosicion(lecturaA); // llega primero la nueva
        rutas.guardarPosicion(lecturaB); // y después la vieja

        Ruta final_ = rutas.buscar(guardada.id()).orElseThrow();
        assertThat(final_.ultimaPosicion()).isEqualTo(nueva);
        assertThat(final_.recorrido()).containsExactly(nueva);
    }
}
