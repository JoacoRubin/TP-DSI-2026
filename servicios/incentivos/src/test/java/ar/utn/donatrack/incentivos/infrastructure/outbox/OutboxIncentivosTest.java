package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.contratos.Colas;
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
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outbox de Incentivos: el aviso de "misión cumplida" se registra en la transacción que otorga la
 * insignia y sale por la cola recién cuando lo despacha {@link DespachadorOutbox}.
 */
class OutboxIncentivosTest {

    private final BackoffOutboxTest.RelojAjustable reloj = new BackoffOutboxTest.RelojAjustable(Instant.parse("2026-11-25T12:00:00Z"));
    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
    private final BrokerFalso broker = new BrokerFalso();
    private final PublicadorColasConOutbox colas = new PublicadorColasConOutbox(bandeja, json, reloj);
    private final DespachadorOutbox despachador = new DespachadorOutbox(bandeja, broker, json, reloj);

    static SolicitudNotificacion misionCumplida(String asunto) {
        return SolicitudNotificacion.de(Destinatario.porEmail("Ana", "ana@donatrack.org"), asunto,
                "Obtuviste la insignia Primer paso", "MISION_CUMPLIDA", "incentivos");
    }

    @Test
    void elAvisoNoSalePorLaColaHastaDespachar() {
        SolicitudNotificacion s = misionCumplida("¡Misión cumplida!");
        colas.publicar(Colas.NOTIFICACIONES, s);

        assertThat(broker.publicados).isEmpty();
        assertThat(bandeja.mensajes).singleElement().satisfies(m -> {
            assertThat(m.estado()).isEqualTo(EstadoMensaje.PENDIENTE);
            assertThat(m.tipo()).isEqualTo("JMS:notificacion.solicitada");
            assertThat(m.clave()).isEqualTo(Colas.NOTIFICACIONES);
        });

        assertThat(despachador.despachar()).isEqualTo(1);
        assertThat(broker.publicados).containsExactly(new Publicado(Colas.NOTIFICACIONES, s));
        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.ENVIADO);

        assertThat(despachador.despachar()).as("no se reenvía lo ya enviado").isZero();
    }

    @Test
    void siElBrokerEstaCaidoQuedaPendienteYSeReintentaEnOrden() {
        SolicitudNotificacion mision = misionCumplida("¡Misión cumplida!");
        SolicitudNotificacion categoria = misionCumplida("¡Subiste de categoría!");
        colas.publicar(Colas.NOTIFICACIONES, mision);
        colas.publicar(Colas.NOTIFICACIONES, categoria);
        broker.caido = true;

        assertThat(despachador.despachar()).isZero();
        assertThat(broker.intentos).as("el segundo espera detrás del primero").isEqualTo(1);
        assertThat(bandeja.mensajes).allMatch(m -> m.estado() == EstadoMensaje.PENDIENTE);
        assertThat(bandeja.mensajes.get(0).intentos()).isEqualTo(1);

        broker.caido = false;
        reloj.avanzar(MensajeSaliente.ESPERA_MAXIMA);
        assertThat(despachador.despachar()).isEqualTo(2);
        assertThat(broker.publicados).extracting(Publicado::mensaje).containsExactly(mision, categoria);
    }

    @Test
    void trasAgotarLosReintentosQuedaFallido() {
        colas.publicar(Colas.NOTIFICACIONES, misionCumplida("¡Misión cumplida!"));
        broker.caido = true;

        for (int i = 0; i < MensajeSaliente.MAX_INTENTOS; i++) {
            despachador.despachar();
            reloj.avanzar(MensajeSaliente.ESPERA_MAXIMA);
        }

        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.FALLIDO);
        assertThat(bandeja.mensajes.get(0).ultimoError()).contains("61616");
    }

    @Test
    void unTipoSinIdentificadorLogicoSeRechazaAlRegistrar() {
        assertThatThrownBy(() -> colas.publicar(Colas.NOTIFICACIONES, "texto suelto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String");
        assertThat(bandeja.mensajes).isEmpty();
    }

    // ------------------------------------------------------------------ dobles

    record Publicado(String cola, Object mensaje) {
    }

    static class BandejaEnMemoria implements BandejaSalida {
        final List<MensajeSaliente> mensajes = new ArrayList<>();
        private long secuencia;

        @Override
        public void guardar(MensajeSaliente m) {
            if (m.id() == null) {
                m.asignarId(++secuencia);
                mensajes.add(m);
            }
        }

        @Override
        public List<MensajeSaliente> pendientes(int limite) {
            return mensajes.stream().filter(m -> m.estado() == EstadoMensaje.PENDIENTE)
                    .sorted(Comparator.comparing(MensajeSaliente::id)).limit(limite).toList();
        }
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
