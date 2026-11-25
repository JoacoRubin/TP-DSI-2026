package ar.utn.donatrack.donaciones.compartido.infrastructure.outbox;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Outbox transaccional hacia Logística: los casos de uso sólo dejan el mensaje en la bandeja
 * (misma transacción que sus cambios) y el despachador lo entrega después, con reintentos.
 */
class OutboxLogisticaTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
    private final ObjectMapper json = new ObjectMapper();
    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
    private final LogisticaFalsa logistica = new LogisticaFalsa();
    private final ServicioLogistica puerto = new ServicioLogisticaConOutbox(bandeja, json, reloj);
    private final DespachadorOutbox despachador = new DespachadorOutbox(bandeja, logistica, new OutboxColasTest.BrokerFalso(), json, reloj);

    private static ServicioLogistica.SolicitudEntrega solicitud(UUID donacionId) {
        return new ServicioLogistica.SolicitudEntrega(donacionId, UUID.randomUUID(), "Ana", UUID.randomUUID(),
                "Escuela 12", "Ruta 6 km 32", -35.05, -58.76, "6 unidades de Sillas", "Sillas", 6, "unidades", 30, 1.2);
    }

    @Test
    void solicitarEntregaNoLlamaALogisticaHastaDespachar() {
        ServicioLogistica.SolicitudEntrega s = solicitud(UUID.randomUUID());
        puerto.solicitarEntrega(s);

        assertThat(logistica.solicitadas).isEmpty();
        assertThat(bandeja.mensajes).singleElement().extracting(MensajeSaliente::estado).isEqualTo(EstadoMensaje.PENDIENTE);

        assertThat(despachador.despachar()).isEqualTo(1);
        assertThat(logistica.solicitadas).containsExactly(s);
        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.ENVIADO);

        assertThat(despachador.despachar()).isZero();
        assertThat(logistica.solicitadas).hasSize(1);
    }

    @Test
    void siLogisticaEstaCaidaQuedaPendienteYSeReintenta() {
        ServicioLogistica.SolicitudEntrega s = solicitud(UUID.randomUUID());
        puerto.solicitarEntrega(s);
        logistica.caida = true;

        assertThat(despachador.despachar()).isZero();
        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(bandeja.mensajes.get(0).intentos()).isEqualTo(1);

        logistica.caida = false;
        assertThat(despachador.despachar()).isEqualTo(1);
        assertThat(logistica.solicitadas).containsExactly(s);
    }

    @Test
    void respetaElOrdenPorDonacionAunqueOtrasDonacionesAvancen() {
        UUID bloqueada = UUID.randomUUID();
        UUID libre = UUID.randomUUID();
        puerto.solicitarEntrega(solicitud(bloqueada));
        puerto.cancelarEntrega(bloqueada);
        puerto.cancelarEntrega(libre);
        logistica.fallarPara = bloqueada;

        despachador.despachar();

        assertThat(logistica.canceladas).containsExactly(libre);

        logistica.fallarPara = null;
        despachador.despachar();
        assertThat(logistica.solicitadas).extracting(ServicioLogistica.SolicitudEntrega::donacionId).containsExactly(bloqueada);
        assertThat(logistica.canceladas).containsExactly(libre, bloqueada);
    }

    @Test
    void unRechazoDeNegocioNoSeReintenta() {
        puerto.cancelarEntrega(UUID.randomUUID());
        logistica.rechazo = true;

        despachador.despachar();
        despachador.despachar();

        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.FALLIDO);
        assertThat(bandeja.mensajes.get(0).intentos()).isEqualTo(1);
        assertThat(logistica.llamadas).isEqualTo(1);
    }

    @Test
    void trasAgotarLosReintentosQuedaFallido() {
        puerto.cancelarEntrega(UUID.randomUUID());
        logistica.caida = true;

        for (int i = 0; i < MensajeSaliente.MAX_INTENTOS; i++) {
            despachador.despachar();
        }

        assertThat(bandeja.mensajes.get(0).estado()).isEqualTo(EstadoMensaje.FALLIDO);
        assertThat(bandeja.mensajes.get(0).ultimoError()).contains("no está disponible");
    }

    // ------------------------------------------------------------------ dobles

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

    static class LogisticaFalsa implements ServicioLogistica {
        final List<SolicitudEntrega> solicitadas = new ArrayList<>();
        final List<UUID> canceladas = new ArrayList<>();
        boolean caida;
        boolean rechazo;
        UUID fallarPara;
        int llamadas;

        private void verificar(UUID donacionId) {
            llamadas++;
            if (rechazo) {
                throw new ReglaDeNegocioException("La entrega no existe");
            }
            if (caida || donacionId.equals(fallarPara)) {
                throw new IllegalStateException("El servicio de logística no está disponible. Intentá nuevamente en unos minutos.");
            }
        }

        @Override
        public void solicitarEntrega(SolicitudEntrega s) {
            verificar(s.donacionId());
            solicitadas.add(s);
        }

        @Override
        public void cancelarEntrega(UUID donacionId) {
            verificar(donacionId);
            canceladas.add(donacionId);
        }
    }
}
