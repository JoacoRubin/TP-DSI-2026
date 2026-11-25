package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
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

/** Reintentos con espera exponencial (3 s, 6 s, 12 s... hasta 5 min) en el outbox de Incentivos. */
class BackoffOutboxTest {

    private final RelojAjustable reloj = new RelojAjustable(Instant.parse("2026-11-25T12:00:00Z"));
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
    private final OutboxIncentivosTest.BandejaEnMemoria bandeja = new OutboxIncentivosTest.BandejaEnMemoria();
    private final OutboxIncentivosTest.BrokerFalso broker = new OutboxIncentivosTest.BrokerFalso();
    private final PublicadorColasConOutbox colas = new PublicadorColasConOutbox(bandeja, json, reloj);
    private final DespachadorOutbox despachador = new DespachadorOutbox(bandeja, broker, json, reloj);

    @Test
    void laEsperaEntreIntentosSeDuplicaHastaElTope() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("aviso"));
        broker.caido = true;

        List<Duration> esperas = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            despachador.despachar();
            Duration espera = Duration.between(reloj.instant(), bandeja.mensajes.get(0).proximoIntento());
            esperas.add(espera);
            reloj.avanzar(espera);
        }

        assertThat(esperas).containsExactly(
                Duration.ofSeconds(3), Duration.ofSeconds(6), Duration.ofSeconds(12), Duration.ofSeconds(24),
                Duration.ofSeconds(48), Duration.ofSeconds(96), Duration.ofSeconds(192),
                MensajeSaliente.ESPERA_MAXIMA, MensajeSaliente.ESPERA_MAXIMA);
    }

    @Test
    void noSeReintentaAntesDeTiempoNiSeAdelantaUnMensajeDeLaMismaCola() {
        colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("primero"));
        broker.caido = true;
        despachador.despachar();
        broker.caido = false;
        colas.publicar(Colas.NOTIFICACIONES, OutboxIncentivosTest.misionCumplida("segundo"));

        reloj.avanzar(Duration.ofSeconds(2));
        assertThat(despachador.despachar()).isZero();
        assertThat(broker.intentos).isEqualTo(1);

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(despachador.despachar()).isEqualTo(2);
        assertThat(broker.publicados).extracting(p -> ((SolicitudNotificacion) p.mensaje()).asunto())
                .containsExactly("primero", "segundo");
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
