package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outbox para los mensajes JMS (notificaciones y eventos a Incentivos): se registran en la
 * transacción del caso de uso y el despachador los publica en la cola recién después.
 */
class OutboxColasTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
    private final OutboxLogisticaTest.BandejaEnMemoria bandeja = new OutboxLogisticaTest.BandejaEnMemoria();
    private final BrokerFalso broker = new BrokerFalso();
    private final PublicadorColasConOutbox colas = new PublicadorColasConOutbox(bandeja, json, reloj);
    private final DespachadorOutbox despachador =
            new DespachadorOutbox(bandeja, new OutboxLogisticaTest.LogisticaFalsa(), broker, json, reloj);

    private static SolicitudNotificacion notificacion(String asunto) {
        return SolicitudNotificacion.de(new Destinatario("Ana", "ana@donatrack.org", null, null, null),
                asunto, "Gracias por tu donación", "DONACION_REGISTRADA", "donaciones");
    }

    @Test
    void laNotificacionNoSalePorLaColaHastaDespachar() {
        SolicitudNotificacion s = notificacion("Recibimos tu donación");
        colas.publicar(Colas.NOTIFICACIONES, s);

        assertThat(broker.publicados).isEmpty();
        assertThat(bandeja.mensajes).singleElement().satisfies(m -> {
            assertThat(m.estado()).isEqualTo(EstadoMensaje.PENDIENTE);
            assertThat(m.clave()).isEqualTo(Colas.NOTIFICACIONES);
        });

        assertThat(despachador.despachar()).isEqualTo(1);
        assertThat(broker.publicados).containsExactly(new Publicado(Colas.NOTIFICACIONES, s));
        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.ENVIADO);
    }

    @Test
    void losEventosAIncentivosConservanSuTipoYSusFechas() {
        EventosDonaciones.DonacionEntregada e = new EventosDonaciones.DonacionEntregada(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), reloj.instant());
        colas.publicar(Colas.INCENTIVOS_DESDE_DONACIONES, e);

        despachador.despachar();

        assertThat(broker.publicados).containsExactly(new Publicado(Colas.INCENTIVOS_DESDE_DONACIONES, e));
    }

    @Test
    void siElBrokerEstaCaidoQuedaPendienteYSeReintentaEnOrden() {
        SolicitudNotificacion primera = notificacion("primera");
        SolicitudNotificacion segunda = notificacion("segunda");
        colas.publicar(Colas.NOTIFICACIONES, primera);
        colas.publicar(Colas.NOTIFICACIONES, segunda);
        broker.caido = true;

        assertThat(despachador.despachar()).isZero();
        assertThat(broker.intentos).as("la segunda espera: misma cola, se respeta el orden").isEqualTo(1);
        assertThat(bandeja.mensajes).allMatch(m -> m.estado() == EstadoMensaje.PENDIENTE);

        broker.caido = false;
        assertThat(despachador.despachar()).isEqualTo(2);
        assertThat(broker.publicados).extracting(Publicado::mensaje).containsExactly(primera, segunda);
    }

    @Test
    void unTipoSinIdentificadorLogicoSeRechazaAlRegistrarYNoAlDespachar() {
        assertThatThrownBy(() -> colas.publicar(Colas.NOTIFICACIONES, "texto suelto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String");
        assertThat(bandeja.mensajes).isEmpty();
    }

    // ------------------------------------------------------------------ dobles

    record Publicado(String cola, Object mensaje) {
    }

    static class BrokerFalso extends PublicadorMensajes {
        final List<Publicado> publicados = new ArrayList<>();
        boolean caido;
        int intentos;

        BrokerFalso() {
            super(null);
        }

        @Override
        public void publicar(String cola, Object mensaje) {
            intentos++;
            if (caido) {
                throw new IllegalStateException("Could not connect to broker URL: tcp://localhost:61616");
            }
            publicados.add(new Publicado(cola, mensaje));
        }
    }
}
