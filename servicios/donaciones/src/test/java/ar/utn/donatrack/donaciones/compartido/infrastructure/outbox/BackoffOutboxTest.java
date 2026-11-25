package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.contratos.Colas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reintentos con espera exponencial: ante una caída, el outbox no martilla al destino cada 3 s
 * sino que espera 3 s, 6 s, 12 s... hasta un tope, y así aguanta caídas largas sin agotar intentos.
 */
class BackoffOutboxTest {

    private final RelojAjustable reloj = new RelojAjustable(Instant.parse("2026-11-25T12:00:00Z"));
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
    private final OutboxLogisticaTest.BandejaEnMemoria bandeja = new OutboxLogisticaTest.BandejaEnMemoria();
    private final OutboxColasTest.BrokerFalso broker = new OutboxColasTest.BrokerFalso();
    private final PublicadorColasConOutbox colas = new PublicadorColasConOutbox(bandeja, json, reloj);
    private final DespachadorOutbox despachador =
            new DespachadorOutbox(bandeja, new OutboxLogisticaTest.LogisticaFalsa(), broker, json, reloj);

    @Test
    void laEsperaEntreIntentosSeDuplicaHastaElTope() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxColasTest.notificacion("aviso"));
        broker.caido = true;

        List<Duration> esperas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            despachador.despachar();
            MensajeSaliente m = bandeja.mensajes.get(0);
            Duration espera = Duration.between(reloj.instant(), m.proximoIntento());
            esperas.add(espera);
            reloj.avanzar(espera);
        }

        assertThat(esperas).containsExactly(
                Duration.ofSeconds(3), Duration.ofSeconds(6), Duration.ofSeconds(12), Duration.ofSeconds(24),
                Duration.ofSeconds(48), Duration.ofSeconds(96), Duration.ofSeconds(192),
                MensajeSaliente.ESPERA_MAXIMA, MensajeSaliente.ESPERA_MAXIMA, MensajeSaliente.ESPERA_MAXIMA);
    }

    @Test
    void noSeReintentaAntesDeTiempo() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxColasTest.notificacion("aviso"));
        broker.caido = true;
        despachador.despachar();
        broker.caido = false;

        reloj.avanzar(Duration.ofSeconds(2));
        assertThat(despachador.despachar()).as("todavía no pasaron los 3 s").isZero();
        assertThat(broker.intentos).isEqualTo(1);

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(despachador.despachar()).isEqualTo(1);
        assertThat(bandeja.mensajes.get(0).proximoIntento()).as("enviado: ya no espera nada").isNull();
    }

    @Test
    void unMensajeNuevoDeLaMismaColaEsperaDetrasDelQueEstaEnEspera() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxColasTest.notificacion("primero"));
        broker.caido = true;
        despachador.despachar();
        broker.caido = false;
        colas.publicar(Colas.NOTIFICACIONES, OutboxColasTest.notificacion("segundo"));

        assertThat(despachador.despachar()).as("el segundo no se adelanta al primero").isZero();

        reloj.avanzar(Duration.ofSeconds(3));
        assertThat(despachador.despachar()).isEqualTo(2);
        assertThat(broker.publicados).extracting(p -> ((ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion) p.mensaje()).asunto())
                .containsExactly("primero", "segundo");
    }

    @Test
    void losVeinteIntentosCubrenMasDeUnaHoraDeCaida() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxColasTest.notificacion("aviso"));
        broker.caido = true;
        Instant inicio = reloj.instant();

        while (bandeja.mensajes.get(0).estado() == EstadoMensaje.PENDIENTE) {
            despachador.despachar();
            MensajeSaliente m = bandeja.mensajes.get(0);
            if (m.proximoIntento() != null) {
                reloj.avanzar(Duration.between(reloj.instant(), m.proximoIntento()));
            }
        }

        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.FALLIDO);
        assertThat(Duration.between(inicio, reloj.instant())).isGreaterThan(Duration.ofHours(1));
    }

    /** Reloj que el test hace avanzar a mano. */
    static class RelojAjustable extends Clock {
        private Instant ahora;

        RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
