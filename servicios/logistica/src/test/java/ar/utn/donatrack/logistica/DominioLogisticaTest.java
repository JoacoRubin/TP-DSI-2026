package ar.utn.donatrack.logistica;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.rutas.domain.Lotes;
import ar.utn.donatrack.logistica.rutas.domain.Parada;
import ar.utn.donatrack.logistica.rutas.domain.Posicion;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioLogisticaTest {

    static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    static Entrega entrega(UUID donanteId) {
        return Entrega.nueva(UUID.randomUUID(), UUID.randomUUID(), donanteId, "Ana", UUID.randomUUID(),
                new Destino("Escuela", "Ruta 6 km 32", -35.05, -58.76),
                new DetalleDonacion("6 unidades de Sillas", "Sillas", 6, "unidades"), 30, 1.2, AHORA);
    }

    static Camion camion() {
        return Camion.registrar(UUID.randomUUID(), "ae 123 bc", 20, 3.2, 3500);
    }

    @Test
    void laPatenteSeNormalizaYSeValidaElFormatoArgentino() {
        assertThat(camion().patente()).isEqualTo("AE123BC");
        assertThat(Camion.registrar(UUID.randomUUID(), "abc123", 10, 2, 1000).patente()).isEqualTo("ABC123");
        assertThatThrownBy(() -> Camion.registrar(UUID.randomUUID(), "XX", 10, 2, 1000)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Camion.registrar(UUID.randomUUID(), "AE123BC", 0, 2, 1000)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void laEntregaRecorreSusEstadosYDejaAuditoria() {
        Entrega e = entrega(UUID.randomUUID());
        Camion c = camion();
        e.enviarAPlanificar(AHORA);
        e.planificar(UUID.randomUUID(), c, AHORA);
        e.iniciarTraslado(AHORA);
        e.confirmarRecepcion(List.of("/api/fotos/a.jpg"), AHORA);

        assertThat(e.estado()).isEqualTo(EstadoEntrega.ENTREGADA);
        assertThat(e.patente()).isEqualTo("AE123BC");
        assertThat(e.fotos()).containsExactly("/api/fotos/a.jpg");
        assertThat(e.historial()).hasSize(5);
    }

    @Test
    void noSePuedeConfirmarUnaEntregaQueNoEstaEnTraslado() {
        Entrega e = entrega(UUID.randomUUID());
        assertThatThrownBy(() -> e.confirmarRecepcion(List.of(), AHORA)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaEntregaNoRecibidaRequiereMotivoYPuedeReprogramarse() {
        Entrega e = entrega(UUID.randomUUID());
        e.enviarAPlanificar(AHORA);
        e.planificar(UUID.randomUUID(), camion(), AHORA);
        e.iniciarTraslado(AHORA);
        assertThatThrownBy(() -> e.informarNoRecibida("", AHORA)).isInstanceOf(ReglaDeNegocioException.class);
        e.informarNoRecibida("Nadie atendió", AHORA);
        assertThat(e.estado()).isEqualTo(EstadoEntrega.NO_RECIBIDA);

        e.reprogramar(AHORA);
        assertThat(e.estado()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(e.rutaId()).isNull();
    }

    @Test
    void lasSolicitudesAlPlanificadorSeParticionanEnLotesDeCien() {
        List<Integer> elementos = IntStream.range(0, 250).boxed().toList();
        List<List<Integer>> lotes = Lotes.partir(elementos, 100);
        assertThat(lotes).extracting(List::size).containsExactly(100, 100, 50);
        assertThat(Lotes.partir(List.of(), 100)).isEmpty();
    }

    private Ruta ruta(UUID donanteId) {
        Entrega e = entrega(donanteId);
        Parada p = Parada.de(1, e.entidadId(), e.destino(), List.of(e));
        return Ruta.planificar(UUID.randomUUID(), LocalDate.of(2026, 11, 17), camion(), List.of(p), AHORA);
    }

    @Test
    void laRutaSoloAceptaPosicionesMientrasEstaEnCurso() {
        Ruta r = ruta(UUID.randomUUID());
        Posicion p = new Posicion(-34.6, -58.4, 40, AHORA);
        assertThatThrownBy(() -> r.registrarPosicion(p, AHORA)).isInstanceOf(ReglaDeNegocioException.class);

        r.iniciar(AHORA);
        assertThat(r.registrarPosicion(p, AHORA)).isTrue();
        assertThat(r.ultimaPosicion()).isEqualTo(p);
    }

    @Test
    void ignoraPosicionesFueraDeOrdenYRechazaLasInvalidas() {
        Ruta r = ruta(UUID.randomUUID());
        r.iniciar(AHORA);
        r.registrarPosicion(new Posicion(-34.6, -58.4, 40, AHORA.plusSeconds(30)), AHORA.plusSeconds(30));

        assertThat(r.registrarPosicion(new Posicion(-34.7, -58.5, 40, AHORA), AHORA.plusSeconds(31))).isFalse();
        assertThatThrownBy(() -> new Posicion(-120, -58.4, 40, AHORA)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> new Posicion(-34.6, -58.4, 500, AHORA)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> r.registrarPosicion(new Posicion(-34.6, -58.4, 40, AHORA.plus(Duration.ofHours(1))), AHORA.plusSeconds(32)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("futuro");
    }

    @Test
    void laRutaSabeQueDonantesEInvolucraYElRecorridoSeAcota() {
        UUID ana = UUID.randomUUID();
        Ruta r = ruta(ana);
        assertThat(r.involucraDonante(ana)).isTrue();
        assertThat(r.involucraDonante(UUID.randomUUID())).isFalse();

        r.iniciar(AHORA);
        for (int i = 0; i < Ruta.MAX_RECORRIDO + 50; i++) {
            r.registrarPosicion(new Posicion(-34.6, -58.4, 30, AHORA.plusSeconds(i)), AHORA.plusSeconds(i));
        }
        assertThat(r.recorrido()).hasSize(Ruta.MAX_RECORRIDO);
    }
}
